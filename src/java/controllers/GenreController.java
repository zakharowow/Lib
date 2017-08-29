package controllers;

import beans.Genre;
import database.Database;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

@ManagedBean(eager = true)
@ApplicationScoped

public class GenreController implements Serializable {

    private ArrayList<Genre> genreList;

    public GenreController() {
        fillGenresAll();
    }

    private void fillGenresAll() {
        Connection conn = null;
        Statement stm = null;
        ResultSet res = null;
        genreList = new ArrayList<>();
        
        try {
            conn = Database.getConnection();
            stm = conn.createStatement();
            res = stm.executeQuery("SELECT * FROM genre ORDER by name");

            while (res.next()) {
                Genre genre = new Genre();
                genre.setName(res.getString("name"));
                genre.setId(res.getLong("id"));
                genreList.add(genre);
            }
        } catch (SQLException ex) {
            Logger.getLogger(GenreController.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(GenreController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public ArrayList<Genre> getGenreList() {
        return genreList;
    }
}
