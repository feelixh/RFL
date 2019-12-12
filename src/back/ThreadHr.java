/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package back;

import frontend.MainForm;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mateu
 */
public class ThreadHr implements Runnable {

    @Override
    public void run() {
        while (true) {

            try {
                SimpleDateFormat data = new SimpleDateFormat("dd/LL/yyyy");
                SimpleDateFormat hora = new SimpleDateFormat("hh:mm:ss");
                data.setTimeZone(TimeZone.getTimeZone("GMT-3:00"));
                hora.setTimeZone(TimeZone.getTimeZone("GMT-3:00"));
                System.out.println(hora.format(new Date()).toString());
                MainForm.setJlHora(hora.format(new Date()).toString());
                MainForm.setJlData(data.format(new Date()).toString());
                Thread.sleep(1000);
            } catch (Exception ex) {
                Logger.getLogger(ThreadHr.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
