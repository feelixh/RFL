/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package start;

import frontend.MainForm;
import java.sql.SQLException;
import java.text.ParseException;

/**
 *
 * @author mateu
 */
public class StartApp {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SQLException, ParseException {
        new MainForm().setVisible(true);
    }
    
}
