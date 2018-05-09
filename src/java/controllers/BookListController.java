package controllers;

import beans.Book;
import database.Database;
import enums.SearchType;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

@ManagedBean(eager = true)
@SessionScoped

public class BookListController implements Serializable {

    //постоянная часть запроса
    private String sqlRequest = "select b.id,b.name,b.isbn,b.page_count,b.publish_year, "
            + "p.name as publisher, a.fio as author, g.name as genre, b.descr, "
            + "b.image from book b inner join author a on b.author_id=a.id "
            + "inner join genre g on b.genre_id=g.id inner join publisher p on b.publisher_id=p.id ";

    private boolean requestFromPager;
    private int pageCount;
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
    //private static Map<String, SearchType> searchMap = new HashMap<String, SearchType>();

    public BookListController() {
        fillBooksAll();
    }

    private void fillBooksBySQL(String sql) {

        StringBuilder sqlBuilder = new StringBuilder(sql);

        currentSql = sql;

        ResultSet res = null;

        try (Connection conn = Database.getConnection();
                Statement stmt = conn.createStatement()) {

            if (!requestFromPager) {
                res = stmt.executeQuery(sql);
                res.last();
                totalBooksCount = res.getRow();
                fillPageNumbers(totalBooksCount, booksOnPage);
            }

            if (totalBooksCount > booksOnPage) {
                sqlBuilder.append(" limit ").append(selectedPageNumber * booksOnPage - booksOnPage).append(",").append(booksOnPage);
            }

            res = stmt.executeQuery(sqlBuilder.toString());

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
                currentBookList.add(book);
            }

        } catch (SQLException ex) {
            Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {

                if (res != null) {
                    res.close();
                }

            } catch (SQLException ex) {
                Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public String updateBooks() throws SQLException {
        imitateLoading();

        ResultSet res = null;

        try (Connection conn = Database.getConnection();
                PreparedStatement prepStmt = conn.prepareStatement("update book set name=?, isbn=?, page_count=?, publish_year=?, descr=? where id=?")) {

            for (Book book : currentBookList) {
                if (!book.isEdit()) {
                    continue;
                }
                prepStmt.setString(1, book.getName());
                prepStmt.setString(2, book.getIsbn());
//                prepStmt.setString(3, book.getAuthor());
                prepStmt.setInt(3, book.getPageCount());
                prepStmt.setInt(4, book.getPublishDate());
//                prepStmt.setString(6, book.getPublisher());
                prepStmt.setString(5, book.getDescr());
                prepStmt.setLong(6, book.getId());
                prepStmt.addBatch();
            }

            prepStmt.executeBatch();

        } catch (SQLException ex) {
            Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        switchEditMode();
        return "books";
    }

    private boolean editMode;

    public boolean isEditMode() {
        return editMode;
    }

    public void switchEditMode() {
        editMode = !editMode;
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

    public void cancelEdit() {
        editMode = false;
        for (Book book : currentBookList) {
            book.setEdit(false);
        }
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
        ResultSet rs = null;

        byte[] content = null;
        try (Connection conn = Database.getConnection();
                Statement stmt = conn.createStatement()) {

            rs = stmt.executeQuery("select content from book where id=" + id);
            while (rs.next()) {
                content = rs.getBytes("content");
            }
        } catch (SQLException ex) {
            Logger.getLogger(Book.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {

                if (rs != null) {
                    rs.close();
                }

            } catch (SQLException ex) {
                Logger.getLogger(Book.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }

        return content;

    }

    public byte[] getImage(int id) {
        ResultSet rs = null;

        byte[] image = null;

        try (Connection conn = Database.getConnection();
                Statement stmt = conn.createStatement()) {

            rs = stmt.executeQuery("select image from book where id=" + id);
            while (rs.next()) {
                image = rs.getBytes("image");
            }

        } catch (SQLException ex) {
            Logger.getLogger(Book.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {

                if (rs != null) {
                    rs.close();
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
        sql.append("where lower(a.fio) like '%" + searchString.toLowerCase() + "%' order by b.name ");
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
        cancelEdit();
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        selectedPageNumber = Integer.valueOf(params.get("page_number"));
        requestFromPager = true;
        fillBooksBySQL(currentSql);
    }

    public void booksOnPageChanged(ValueChangeEvent e) {
        imitateLoading();
        cancelEdit();
        requestFromPager = false;
        booksOnPage = Integer.valueOf(e.getNewValue().toString());
        selectedPageNumber = 1;
        fillBooksBySQL(currentSql);
    }

    private void fillPageNumbers(long totalBooksCount, int booksCountOnPage) {
        if (totalBooksCount > 0) {
            int fpc = (int) totalBooksCount / booksCountOnPage;
            pageCount = (int) totalBooksCount % booksCountOnPage == 0 ? fpc : fpc + 1;
        }

        pageNumbers.clear();
        for (int i = 1; i <= pageCount; i++) {
            pageNumbers.add(i);
        }

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

    public void searchTypeChanged(ValueChangeEvent e) {
        searchType = (SearchType) e.getNewValue();
    }

    public void searchStringChanged(ValueChangeEvent e) {
        searchString = e.getNewValue().toString();
    }

}
