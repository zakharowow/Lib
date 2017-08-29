package controllers;

import beans.Book;
import database.Database;
import enums.SearchType;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

@ManagedBean(eager = true)
@SessionScoped

public class BookListController implements Serializable {

    //постоянная часть запроса
    private String sqlRequest = "select b.id,b.name,b.isbn,b.page_count,b.publish_year, "
            + "p.name as publisher, a.fio as author, g.name as genre, b.descr, "
            + "b.image from book b inner join author a on b.author_id=a.id "
            + "inner join genre g on b.genre_id=g.id inner join publisher p on b.publisher_id=p.id ";

    private boolean requestFromPager;
    private int booksOnPage = 2;
    private int selectedGenreId; // выбранный жанр
    private char selectedLetter; // выбранная буква алфавита
    private long selectedPageNumber = 1; // выбранный номер страницы в постраничной навигации
    private long totalBooksCount; // общее кол-во книг (не на текущей странице, а всего), нужно для постраничности
    private ArrayList<Integer> pageNumbers = new ArrayList<Integer>(); // общее кол-во страниц
    private SearchType searchType;// хранит выбранный тип поиска
    private String searchString; // хранит поисковую строку
    private ArrayList<Book> currentBookList; // текущий список книг для отображения
    private String currentSql;// последний выполнный sql без добавления limit
    // хранит все виды поисков (по автору, по названию)
    private static Map<String, SearchType> searchMap = new HashMap<String, SearchType>();

    public BookListController() {
        fillBooksAll();
        ResourceBundle bundle = ResourceBundle.getBundle("nls.properties", FacesContext.getCurrentInstance().getViewRoot().getLocale());
        searchMap.put(bundle.getString("author_name"), searchType.AUTHOR);
        searchMap.put(bundle.getString("book_name"), searchType.TITLE);
    }

    private void fillBooksBySQL(String sql) {

        StringBuilder sqlBuilder = new StringBuilder(sql);
        currentSql = sql;

        Connection conn = null;
        Statement stm = null;
        ResultSet res = null;

        try {
            conn = Database.getConnection();
            stm = conn.createStatement();

            if (!requestFromPager) {
                res = stm.executeQuery(sql);
                res.last();
                totalBooksCount = res.getRow();
                fillPageNumbers(totalBooksCount, booksOnPage);
            }

            if (totalBooksCount > booksOnPage) {
                sqlBuilder.append(" limit ").append(selectedPageNumber * booksOnPage - booksOnPage).append(",").append(booksOnPage);
            }

            res = stm.executeQuery(sqlBuilder.toString());

            currentBookList = new ArrayList<>();

            while (res.next()) {
                Book book = new Book();
                book.setId(res.getLong("id"));
                book.setName(res.getString("name"));
                book.setGenre(res.getString("name"));
                book.setIsbn(res.getString("isbn"));
                book.setAuthor(res.getString("author"));
                book.setPageCount(res.getInt("page_count"));
                book.setPublishDate(res.getInt("publish_year"));
                book.setPublisher(res.getString("publisher"));
                book.setDescr(res.getString("descr"));
                //ook.setImage(res.getBytes("image"));
                //book.setContent(res.getBytes("content"));
                currentBookList.add(book);
            }

        } catch (SQLException ex) {
            Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stm != null) {
                    stm.close();
                }
                if (res != null) {
                    res.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void fillBooksAll() {
        fillBooksBySQL(sqlRequest + "order by b.name");
    }

    private void submitValues(Character selectedLetter, long selectedPageNumber, int selectedGenreId, boolean requestFromPager) {
        this.selectedLetter = selectedLetter;
        this.selectedPageNumber = selectedPageNumber;
        this.selectedGenreId = selectedGenreId;
        this.requestFromPager = requestFromPager;
    }

    public String fillBooksByGenre() {
        imitateLoading();
        
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        submitValues(' ', 1, Integer.valueOf(params.get("genre_id")), false);

        fillBooksBySQL(sqlRequest + "where genre_id=" + selectedGenreId + " order by b.name");

        return "books";
    }

    public Character[] getRussianLetters() {

        Character[] letters = {'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И', 'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ы', 'Э', 'Ю', 'Я'};
        return letters;
    }

    public String fillBooksByLetter() {
        imitateLoading();
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        selectedLetter = params.get("letter").charAt(0);

        submitValues(selectedLetter, 1, -1, false);

        fillBooksBySQL(sqlRequest + "where substr(b.name,1,1)='" + selectedLetter + "' order by b.name");
        return "books";
    }

    public byte[] getContent(int id) {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        byte[] content = null;
        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("select content from book where id=" + id);
            while (rs.next()) {
                content = rs.getBytes("content");
            }
        } catch (SQLException ex) {
            Logger.getLogger(Book.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(Book.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }

        return content;

    }

    public byte[] getImage(int id) {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        byte[] image = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("select image from book where id=" + id);
            while (rs.next()) {
                image = rs.getBytes("image");
            }

        } catch (SQLException ex) {
            Logger.getLogger(Book.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(Book.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }

        return image;
    }

    public String fillBooksBySearch() {
        imitateLoading();
        
        submitValues(' ', 1, -1, false);

        if (searchString.trim().length() == 0) {
            fillBooksAll();
            return "books";
        }

        StringBuilder sql = new StringBuilder(sqlRequest);
        switch (searchType) {
            case AUTHOR:
                sql.append("where lower(a.fio) like '%" + searchString.toLowerCase() + "%' order by b.name ");
                break;
            case TITLE:
                sql.append("where lower(b.name) like '%" + searchString.toLowerCase() + "%' order by b.name ");
                break;
        }

        fillBooksBySQL(sql.toString());

        return "books";
    }

    public void selectPage() {
        imitateLoading();
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        selectedPageNumber = Integer.valueOf(params.get("page_number"));
        requestFromPager = true;
        fillBooksBySQL(currentSql);
    }

    private void fillPageNumbers(long totalBooksCount, int booksCountOnPage) {
        int pageCount = 0;
        if (totalBooksCount > 0) {
            int fpc = (int) totalBooksCount / booksCountOnPage;
            pageCount = (int) totalBooksCount % booksCountOnPage == 0 ? fpc : fpc + 1;
        }

        pageNumbers.clear();
        for (int i = 1; i <= pageCount; i++) {
            pageNumbers.add(i);
        }

    }

    public Map<String, SearchType> getSearchMap() {
        return searchMap;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public ArrayList<Book> getCurrentBookList() {
        return currentBookList;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public ArrayList<Integer> getPageNumbers() {
        return pageNumbers;
    }

    public void setPageNumbers(ArrayList<Integer> pageNumbers) {
        this.pageNumbers = pageNumbers;
    }

    public long getTotalBooksCount() {
        return totalBooksCount;
    }

    public void setTotalBooksCount(long totalBooksCount) {
        this.totalBooksCount = totalBooksCount;
    }

    public int getBooksOnPage() {
        return booksOnPage;
    }

    public void setBooksOnPage(int booksOnPage) {
        this.booksOnPage = booksOnPage;
    }

    public long getSelectedPageNumber() {
        return selectedPageNumber;
    }

    public void setSelectedPageNumber(long selectedPageNumber) {
        this.selectedPageNumber = selectedPageNumber;
    }

    public int getSelectedGenreId() {
        return selectedGenreId;
    }

    public void setSelectedGenreId(int selectedGenreId) {
        this.selectedGenreId = selectedGenreId;
    }

    public char getSelectedLetter() {
        return selectedLetter;
    }

    public void setSelectedLetter(char selectedLetter) {
        this.selectedLetter = selectedLetter;
    }

    private void imitateLoading() {
        try {
            Thread.sleep(500);// имитация загрузки процесса
        } catch (InterruptedException ex) {
            Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
