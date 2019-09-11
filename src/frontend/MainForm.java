/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package frontend;

import java.io.IOException;
import org.jdesktop.application.Action;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import Luxand.*;
import Luxand.FSDK.*;
import Luxand.FSDKCam.*;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author mateu
 */
public class MainForm extends javax.swing.JFrame {

    String pathConfig = "config.json";
    String configs = "";
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private ResultSetMetaData metaData;
    private int numberOfRows;
    private boolean connectedToDatabase = false;
    static final String url = "jdbc:mysql://187.109.226.100/pjiii";
    static final String userBanco = "pjiii";
    static final String pwBanco = "pjiii2019";
JPanel jpAgradecimentos;
    /**
     * Creates new form MainForm
     */
    public MainForm() throws SQLException, ParseException {

        initComponents();
        javax.swing.text.MaskFormatter cpf = new javax.swing.text.MaskFormatter("###.###.###-##");
        cpf.install(jftCPF);
         jpAgradecimentos = new JPanel();
        jpAgradecimentos.setBounds(jpCadastrar.getX(), jpCadastrar.getY(), jpCadastrar.getWidth(), jpCadastrar.getHeight());
        jpCadastrar.setVisible(false);

        jpAgradecimentos.setBorder(jpCadastrar.getBorder());
        jpAgradecimentos.setVisible(true);
        final JPanel mainFrame = this.jpReconhecimento;
        getConn(url, userBanco, pwBanco);
        resultSet = statement.executeQuery("select * from unijui");
        metaData = resultSet.getMetaData();
        resultSet.last();
        System.out.println("testeee nunmero de linhas da consulta: " + resultSet.getRow());
        jpCadastrar.setVisible(false);
        try {
            int r = FSDK.ActivateLibrary(this.getKey());
            if (r != FSDK.FSDKE_OK) {
                JOptionPane.showMessageDialog(jpReconhecimento, "Please run the License Key Wizard (Start - Luxand - FaceSDK - License Key Wizard)", "Error activating FaceSDK", JOptionPane.ERROR_MESSAGE);
                System.exit(r);
            }
        } catch (java.lang.UnsatisfiedLinkError e) {
            JOptionPane.showMessageDialog(jpReconhecimento, e.toString(), "Link Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        FSDK.Initialize();

        // creating a Tracker
        if (FSDK.FSDKE_OK != FSDK.LoadTrackerMemoryFromFile(tracker, TrackerMemoryFile)) // try to load saved tracker state
        {
            FSDK.CreateTracker(tracker); // if could not be loaded, create a new tracker
        }
        // set realtime face detection parameters
        int err[] = new int[1];
        err[0] = 0;
        FSDK.SetTrackerMultipleParameters(tracker, "HandleArbitraryRotations=false; DetermineFaceRotationAngle=false; InternalResizeWidth=100; FaceDetectionThreshold=5;", err);

        FSDKCam.InitializeCapturing();

        TCameras cameraList = new TCameras();
        int count[] = new int[1];
        FSDKCam.GetCameraList(cameraList, count);
        if (count[0] == 0) {
            JOptionPane.showMessageDialog(mainFrame, "Please attach a camera");
            System.exit(1);
        }

        String cameraName = cameraList.cameras[0];

        FSDK_VideoFormats formatList = new FSDK_VideoFormats();
        FSDKCam.GetVideoFormatList(cameraName, formatList, count);
        FSDKCam.SetVideoFormat(cameraName, formatList.formats[0]);

        cameraHandle = new HCamera();

        int r = FSDKCam.OpenVideoCamera(cameraName, cameraHandle);
        if (r != FSDK.FSDKE_OK) {
            JOptionPane.showMessageDialog(mainFrame, "Error opening camera");
            System.exit(r);
        }

        //FSDK.ClearTracker(tracker); // ESSA LINHA LIMPA TODAS AS FACES CADASTRADAS NO TRACKER
        // Timer to draw and process image from camera
        drawingTimer = new Timer(40, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                HImage imageHandle = new HImage();
                List a = new ArrayList<HImage>();

                if (FSDKCam.GrabFrame(cameraHandle, imageHandle) == FSDK.FSDKE_OK) {
                    Image awtImage[] = new Image[1];
                    if (FSDK.SaveImageToAWTImage(imageHandle, awtImage, FSDK_IMAGEMODE.FSDK_IMAGE_COLOR_24BIT) == FSDK.FSDKE_OK) {

                        BufferedImage bufImage = null;
                        TFacePosition.ByReference facePosition = new TFacePosition.ByReference();

                        long[] IDs = new long[256]; // maximum of 256 faces detected
                        long[] faceCount = new long[1];

                        FSDK.FeedFrame(tracker, 0, imageHandle, faceCount, IDs);
                        if (!(faceCount[0] > 0)) {
                           jpCadastrar.setVisible(false);
                           jpAgradecimentos.setVisible(true);

                        }
                        for (int i = 0; i < faceCount[0]; ++i) {
                            FSDK.GetTrackerFacePosition(tracker, 0, IDs[i], facePosition);

                            int left = facePosition.xc - (int) (facePosition.w * 0.6);
                            int top = facePosition.yc - (int) (facePosition.w * 0.5);
                            int w = (int) (facePosition.w * 1.2);

                            bufImage = new BufferedImage(awtImage[0].getWidth(null), awtImage[0].getHeight(null), BufferedImage.TYPE_INT_ARGB);
                            Graphics gr = bufImage.getGraphics();
                            gr.drawImage(awtImage[0], 0, 0, null);
                            gr.setColor(Color.green);

                            String[] name = new String[1];
                            int res = FSDK.GetAllNames(tracker, IDs[i], name, 65536); // maximum of 65536 characters

                            if (FSDK.FSDKE_OK == res && name[0].length() > 0) { // draw name
                                gr.setFont(new Font("Arial", Font.BOLD, 16));
                                FontMetrics fm = gr.getFontMetrics();
                                java.awt.geom.Rectangle2D textRect = fm.getStringBounds(name[0], gr);
                                gr.drawString(name[0], (int) (facePosition.xc - textRect.getWidth() / 2), (int) (top + w + textRect.getHeight()));
                                jpCadastrar.setVisible(false);
                            } else { //se for desconhecido
                                gr.setFont(new Font("Arial", Font.BOLD, 16));
                                FontMetrics fm = gr.getFontMetrics();
                                java.awt.geom.Rectangle2D textRect = fm.getStringBounds("Desconhecido", gr);
                                gr.drawString("Desconhecido", (int) (facePosition.xc - textRect.getWidth() / 2), (int) (top + w + textRect.getHeight()));
                                jpCadastrar.setVisible(true);

                                // aqui a ideia é que metade da tela apresente a câmera, e outra metade um forms para cadastro
                            }

                            if (mouseX >= left && mouseX <= left + w && mouseY >= top && mouseY <= top + w) {
                                gr.setColor(Color.blue);

                                if (programStateRemember == programState) {
                                    if (FSDK.FSDKE_OK == FSDK.LockID(tracker, IDs[i])) {

                                        // get the user name
                                        userName = (String) JOptionPane.showInputDialog(mainFrame, "Your name:", "Enter your name", JOptionPane.QUESTION_MESSAGE, null, null, "User");
                                        FSDK.SetName(tracker, IDs[i], userName);
                                        if (userName == null || userName.length() <= 0) {
                                            FSDK.PurgeID(tracker, IDs[i]);
                                        }
                                        FSDK.UnlockID(tracker, IDs[i]);
                                    }
                                }
                            }
                            programState = programStateRecognize;

                            gr.drawRect(left, top, w, w); // draw face rectangle
                        }

                        // display current frame
                        mainFrame.getRootPane().getGraphics().drawImage((bufImage != null) ? bufImage : awtImage[0], 50, 50, null);
                    }
                    FSDK.FreeImage(imageHandle); // delete the FaceSDK image handle
                }
            }
        });
        drawingTimer.start();
    }

    private void getConn(String url, String user, String password) throws SQLException {
        connection = DriverManager.getConnection(url, user, password);
        statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    }

    private String getKey() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(".").getCanonicalPath() + "\\" + pathConfig));
            while (br.ready()) {
                configs += br.readLine();
            }
            br.close();
            System.out.println(configs);

            JSONObject json = new JSONObject(configs);

            System.out.println(json.getString("key"));
            return json.getString("key");
        } catch (Exception ex) {
            JOptionPane.showInternalMessageDialog(null, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);

        }
        return "null";
    }

    public void saveTracker() {
        FSDK.SaveTrackerMemoryToFile(tracker, TrackerMemoryFile);
    }

    public void closeCamera() {
        FSDKCam.CloseVideoCamera(cameraHandle);
        FSDKCam.FinalizeCapturing();
        FSDK.Finalize();
    }

    public void saveUser(long id, long cpf, ArrayList<HImage> imagens) {

        System.out.println("Nome da pessoa: " + "CPF: " + cpf + "\n" + "\n" + "ID do tracker: " + id + "\n" + "Número de imagens salvas da pessoa: " + imagens.size());

        // int m= FSDK.SaveImageToFile(imageHandle, "teste.jpg"); // coloquei pra teste, salvar img usar dps
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpCadastrar = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jftCPF = new javax.swing.JFormattedTextField();
        jb1 = new javax.swing.JButton();
        jb2 = new javax.swing.JButton();
        jb3 = new javax.swing.JButton();
        jb4 = new javax.swing.JButton();
        jb5 = new javax.swing.JButton();
        jb6 = new javax.swing.JButton();
        jb7 = new javax.swing.JButton();
        jb8 = new javax.swing.JButton();
        jb9 = new javax.swing.JButton();
        jbOk = new javax.swing.JButton();
        jb0 = new javax.swing.JButton();
        jbApagar = new javax.swing.JButton();
        jpReconhecimento = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        jpCadastrar.setBorder(javax.swing.BorderFactory.createTitledBorder("Cadastrar"));
        jpCadastrar.setForeground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("CADASTRE-SE AQUI");

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Insira seu CPF");
        jLabel2.setToolTipText("");

        jftCPF.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jftCPF.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N

        jb1.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb1.setText("1");
        jb1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb2.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb2.setText("2");
        jb2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb3.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb3.setText("3");
        jb3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb3ActionPerformed(evt);
            }
        });

        jb4.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb4.setText("4");
        jb4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb5.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb5.setText("5");
        jb5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb6.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb6.setText("6");
        jb6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb6ActionPerformed(evt);
            }
        });

        jb7.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb7.setText("7");
        jb7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb8.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb8.setText("8");
        jb8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb9.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb9.setText("9");
        jb9.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb9ActionPerformed(evt);
            }
        });

        jbOk.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jbOk.setText("V");
        jbOk.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jbOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbOkActionPerformed(evt);
            }
        });

        jb0.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb0.setText("0");
        jb0.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jbApagar.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jbApagar.setText("X");
        jbApagar.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jpCadastrarLayout = new javax.swing.GroupLayout(jpCadastrar);
        jpCadastrar.setLayout(jpCadastrarLayout);
        jpCadastrarLayout.setHorizontalGroup(
            jpCadastrarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpCadastrarLayout.createSequentialGroup()
                .addGroup(jpCadastrarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jftCPF, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                    .addGroup(jpCadastrarLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpCadastrarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpCadastrarLayout.createSequentialGroup()
                                .addGap(0, 149, Short.MAX_VALUE)
                                .addGroup(jpCadastrarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jpCadastrarLayout.createSequentialGroup()
                                        .addComponent(jb1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jb2))
                                    .addGroup(jpCadastrarLayout.createSequentialGroup()
                                        .addComponent(jb4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jb5))
                                    .addGroup(jpCadastrarLayout.createSequentialGroup()
                                        .addComponent(jb7)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jb8))
                                    .addGroup(jpCadastrarLayout.createSequentialGroup()
                                        .addComponent(jbApagar)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jb0)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jpCadastrarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jb3)
                                    .addComponent(jb6)
                                    .addComponent(jb9)
                                    .addComponent(jbOk))
                                .addGap(120, 120, 120)))))
                .addContainerGap())
        );
        jpCadastrarLayout.setVerticalGroup(
            jpCadastrarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpCadastrarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jftCPF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jpCadastrarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb1)
                    .addComponent(jb2)
                    .addComponent(jb3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jpCadastrarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb4)
                    .addComponent(jb5)
                    .addComponent(jb6))
                .addGap(12, 12, 12)
                .addGroup(jpCadastrarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb7)
                    .addComponent(jb8)
                    .addComponent(jb9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jpCadastrarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbApagar)
                    .addComponent(jb0)
                    .addComponent(jbOk))
                .addContainerGap(232, Short.MAX_VALUE))
        );

        jpReconhecimento.setForeground(new java.awt.Color(255, 255, 255));
        jpReconhecimento.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jpReconhecimentoMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jpReconhecimentoMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jpReconhecimentoMouseReleased(evt);
            }
        });
        jpReconhecimento.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jpReconhecimentoMouseMoved(evt);
            }
        });

        javax.swing.GroupLayout jpReconhecimentoLayout = new javax.swing.GroupLayout(jpReconhecimento);
        jpReconhecimento.setLayout(jpReconhecimentoLayout);
        jpReconhecimentoLayout.setHorizontalGroup(
            jpReconhecimentoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 662, Short.MAX_VALUE)
        );
        jpReconhecimentoLayout.setVerticalGroup(
            jpReconhecimentoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 562, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(138, Short.MAX_VALUE)
                .addComponent(jpReconhecimento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(49, 49, 49)
                .addComponent(jpCadastrar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jpCadastrar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jpReconhecimento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 72, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jb3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jb3ActionPerformed

    private void jb6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb6ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jb6ActionPerformed

    private void jb9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb9ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jb9ActionPerformed

    private void jbOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbOkActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jbOkActionPerformed

    private void jpReconhecimentoMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jpReconhecimentoMouseMoved
        mouseX = evt.getX();
        mouseY = evt.getY();        // TODO add your handling code here:
    }//GEN-LAST:event_jpReconhecimentoMouseMoved

    private void jpReconhecimentoMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jpReconhecimentoMouseExited
        mouseX = 0;
        mouseY = 0;        // TODO add your handling code here:
    }//GEN-LAST:event_jpReconhecimentoMouseExited

    private void jpReconhecimentoMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jpReconhecimentoMouseReleased
        programState = programStateRemember;
    }//GEN-LAST:event_jpReconhecimentoMouseReleased

    private void jpReconhecimentoMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jpReconhecimentoMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_jpReconhecimentoMouseEntered

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton jb0;
    private javax.swing.JButton jb1;
    private javax.swing.JButton jb2;
    private javax.swing.JButton jb3;
    private javax.swing.JButton jb4;
    private javax.swing.JButton jb5;
    private javax.swing.JButton jb6;
    private javax.swing.JButton jb7;
    private javax.swing.JButton jb8;
    private javax.swing.JButton jb9;
    private javax.swing.JButton jbApagar;
    private javax.swing.JButton jbOk;
    private javax.swing.JFormattedTextField jftCPF;
    private javax.swing.JPanel jpCadastrar;
    private javax.swing.JPanel jpReconhecimento;
    // End of variables declaration//GEN-END:variables

    public final Timer drawingTimer;
    private HCamera cameraHandle;
    private String userName;

    private List<FSDK_FaceTemplate.ByReference> faceTemplates; // set of face templates (we store 10)

    // program states: waiting for the user to click a face
    // and recognizing user's face
    final int programStateRemember = 1;
    final int programStateRecognize = 2;
    private int programState = programStateRecognize;

    private String TrackerMemoryFile = "tracker70.dat";
    private int mouseX = 0;
    private int mouseY = 0;

    HTracker tracker = new HTracker();
}
