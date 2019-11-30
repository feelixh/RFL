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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
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
    boolean jbOKPressed = false;
    boolean jbOkAlunoPressed = false;
    private boolean stateSave = false;
    private long lastIdDetected = -1;
    private HashMap<String, String> currentUser = null;
    private boolean jbTentarNovamentePressed = false;
    Random rand;

    /**
     * Creates new form MainForm
     */
    public MainForm() throws SQLException, ParseException {
        initComponents();

        /*
        String t = "1-117064";
        System.out.println(t.substring(t.indexOf("-")+1,t.length()));
        System.exit(0);
        //testes
         */
        rand = new Random(1000);
         
        jtbMain.setSelectedIndex(1);
        jtbMain.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tab_placement, int run_count, int max_tab_height) {
                if (jtbMain.getTabCount() > 1) {
                    jtbMain.setBounds((getWidth()/2), (getHeight() - Math.round((float) getHeight() / 1.5f)) / 2,(getWidth() / 2) - 20, Math.round((float) getHeight() / 1.5f));
                    //jpInfos.setSize(jtbMain.getWidth(), jtbMain.getHeight());
                    return super.calculateTabAreaHeight(tab_placement, run_count, -10);
                    
                } else {
                    return 0;
                }
            }
        });
       
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        javax.swing.text.MaskFormatter cpf = new javax.swing.text.MaskFormatter("###.###.###-##");
        cpf.install(jftCPF1);

        final JPanel mainFrame = this.jpReconhecimento;
        
        
        getConn(url, userBanco, pwBanco);
        resultSet = statement.executeQuery("select * from unijui where matr_aluno = 117064;");
        metaData = resultSet.getMetaData();
        resultSet.last();
        System.out.println("erstr nunmero de linhas da consulta: " + resultSet.getNString("nome_aluno"));
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
                                if (lastIdDetected != IDs[i]) {
                                    lastIdDetected = IDs[i];
                                    currentUser = verifyUser(IDs[i], "img/" + name[0].substring(name[0].indexOf("-") + 1, name[0].length()));

                                }
                                if (currentUser != null) {
                                    if (currentUser.containsKey("ativo")) {
                                        if (Integer.parseInt(currentUser.get("ativo")) == 1) {
                                            gr.setFont(new Font("Arial", Font.BOLD, 16));
                                            FontMetrics fm = gr.getFontMetrics();
                                            java.awt.geom.Rectangle2D textRect = fm.getStringBounds(currentUser.get("nome"), gr);
                                            gr.drawString(currentUser.get("nome"), (int) (facePosition.xc - textRect.getWidth() / 2), (int) (top + w + textRect.getHeight()));
                                            System.out.println(currentUser.get("ativo"));
                                            System.out.println(currentUser.get("nome"));
                                            setPanelOk(currentUser.get("nome"), currentUser.get("materia"));
                                        } else if (Integer.parseInt(currentUser.get("ativo")) == 2) {
                                            setPanelReprovado();
                                            if (jbTentarNovamentePressed) {
                                                jbTentarNovamentePressed = false;
                                                removeUser(IDs[i], "img/" + name[0].substring(name[0].indexOf("-") + 1, name[0].length()));
                                                FSDK.PurgeID(tracker, IDs[i]);
                                                saveTracker();
                                                //remover foto do hd tbm
                                                currentUser = null;
                                                lastIdDetected = -1;
                                                setPanelCadastroRG();

                                            }
                                        } else {
                                            gr.setFont(new Font("Arial", Font.BOLD, 16));
                                            FontMetrics fm = gr.getFontMetrics();
                                            java.awt.geom.Rectangle2D textRect = fm.getStringBounds(name[0], gr);
                                            gr.drawString(name[0], (int) (facePosition.xc - textRect.getWidth() / 2), (int) (top + w + textRect.getHeight()));
                                            System.out.println("id salvo: " + IDs[i]);
                                            System.out.println("tracker salvo: " + tracker.toString());
                                            setPanelAguardando();
                                        }

                                    }
                                }

                            } else { //se for desconhecido
                                setPanelCadastroRG();
                                gr.setFont(new Font("Arial", Font.BOLD, 16));
                                FontMetrics fm = gr.getFontMetrics();
                                java.awt.geom.Rectangle2D textRect = fm.getStringBounds("Desconhecido", gr);
                                gr.drawString("Desconhecido", (int) (facePosition.xc - textRect.getWidth() / 2), (int) (top + w + textRect.getHeight()));

                                // aqui a ideia é que metade da tela apresente a câmera, e outra metade um forms para cadastro
                            }

                            if (stateSave) {
                                System.out.println("entrou no save");
                                stateSave = false;
                                gr.setColor(Color.blue);
                                if (programStateRemember == programState && jbOkAlunoPressed == true) {
                                    if (FSDK.FSDKE_OK == FSDK.LockID(tracker, IDs[i])) {
                                        jbOkAlunoPressed = false;
                                        userName = Long.toString(IDs[i]) + "-" + getRG();
                                        FSDK.SetName(tracker, IDs[i], userName);
                                        FSDK.UnlockID(tracker, IDs[i]);
                                        saveTracker();
                                        System.out.println("tr " + tracker);
                                        System.out.println("id " + IDs[i]);
                                        FSDK.SaveImageToFile(imageHandle, "img/" + getRG() + ".jpg");
                                        saveUser(IDs[i], getRG(), "img/" + getRG() + ".jpg");

                                    }
                                }
                            }
                            programState = programStateRecognize;

                            gr.drawRect(left, top, w, w); // draw face rectangle
                        }

                        // display current frame
                        mainFrame.getRootPane().getGraphics().drawImage((bufImage != null) ? bufImage : awtImage[0], 20, (getHeight() - Math.round((float) getHeight() / 1.5f)) / 2, (getWidth() / 2) - 20, Math.round((float) getHeight() / 1.5f), null);
                        
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

    private String getCPF() {
        if (jftCPF1.getText().length() > 0) {
            return jftCPF1.getText();

        } else {

        }
        return "";
    }

    private String getRG() {
        if (jftRGAluno.getText().length() > 0) {
            return jftRGAluno.getText();

        } else {

        }
        return "";
    }

    private boolean saveUser(long id, String cpf, String pathImagem) {
        try {

            String sql = "insert into reconhecimento (rg_aluno, tracker, imagem) values ('" + cpf + "','" + Long.toString(id) + "','" + pathImagem + "');";
            System.out.println(sql);
            if (connection.isClosed()) {
                getConn(url, userBanco, pwBanco);
                if (statement.execute(sql)) {
                    System.out.println("inseriu no banco com sucesso");
                    return true;
                }
            } else {

                if (statement.execute(sql)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private HashMap verifyUser(long id, String pathImagem) {
        HashMap<String, String> userInfo = new HashMap<String, String>();
        try {
            String sql = "select * from reconhecimento as r inner join unijui as u on u.matr_aluno = r.rg_aluno where r.tracker = " + Long.toString(id) + " and r.imagem like '" + pathImagem + "%';";
            System.out.println(sql);
            if (connection.isClosed()) {
                getConn(url, userBanco, pwBanco);
                resultSet = statement.executeQuery(sql);
                userInfo.put("ativo", resultSet.getString("ativo"));
                userInfo.put("nome", resultSet.getString("nome_aluno"));
                userInfo.put("materia", resultSet.getString("nome_ativ_curric"));
                return userInfo;

            } else {

                resultSet = statement.executeQuery(sql);
                resultSet.last();
                userInfo.put("ativo", resultSet.getString("ativo"));
                userInfo.put("nome", resultSet.getString("nome_aluno"));
                userInfo.put("materia", resultSet.getString("nome_ativ_curric"));
                return userInfo;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return userInfo;
        }
    }

    private boolean removeUser(long id, String pathImagem) {
        try {

            String sql = "delete from reconhecimento where ativo = 2 and tracker = " + Long.toString(id) + " and imagem like '" + pathImagem + "%';";
            System.out.println(sql);
            if (connection.isClosed()) {
                getConn(url, userBanco, pwBanco);
                if (statement.execute(sql)) {
                    return true;
                }
            } else {

                if (statement.execute(sql)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void setPanelOk(String nome, String materia) {
        jlBemvindo.setText("Olá " + nome.substring(0, nome.indexOf(" ")));
        jlDisciplina.setText(materia);
        jtbMain.setSelectedIndex(1);
    }

    private void setPanelAguardando() {
        jtbMain.setSelectedIndex(3);
    }

    private void setPanelReprovado() {
        jtbMain.setSelectedIndex(4);
    }

    private void setPanelCadastroRG() {
        jtbMain.setSelectedIndex(0);
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

    public int getHeight() {
        return this.getContentPane().getHeight();
    }

    public int getWidth() {
        return this.getContentPane().getWidth();
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

        jpReconhecimento = new javax.swing.JPanel();
        jtbMain = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jbApagar2 = new javax.swing.JButton();
        jb20 = new javax.swing.JButton();
        jbOkAluno = new javax.swing.JButton();
        jb21 = new javax.swing.JButton();
        jb22 = new javax.swing.JButton();
        jb23 = new javax.swing.JButton();
        jb24 = new javax.swing.JButton();
        jb25 = new javax.swing.JButton();
        jb26 = new javax.swing.JButton();
        jb27 = new javax.swing.JButton();
        jb28 = new javax.swing.JButton();
        jb29 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jftRGAluno = new javax.swing.JFormattedTextField();
        jpInfos = new javax.swing.JPanel();
        jlBemvindo = new javax.swing.JLabel();
        jLabelAula = new javax.swing.JLabel();
        jlDisciplina = new javax.swing.JLabel();
        jLabelSala = new javax.swing.JLabel();
        jlSala = new javax.swing.JLabel();
        jlDescricao = new javax.swing.JLabel();
        jpCadastrar1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jftCPF1 = new javax.swing.JFormattedTextField();
        jb10 = new javax.swing.JButton();
        jb11 = new javax.swing.JButton();
        jb12 = new javax.swing.JButton();
        jb13 = new javax.swing.JButton();
        jb14 = new javax.swing.JButton();
        jb15 = new javax.swing.JButton();
        jb16 = new javax.swing.JButton();
        jb17 = new javax.swing.JButton();
        jb18 = new javax.swing.JButton();
        jbOk1 = new javax.swing.JButton();
        jb19 = new javax.swing.JButton();
        jbApagar1 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jbTentarNovamente = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));
        setUndecorated(true);

        jpReconhecimento.setForeground(new java.awt.Color(255, 255, 255));
        jpReconhecimento.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jpReconhecimentoMouseMoved(evt);
            }
        });
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

        javax.swing.GroupLayout jpReconhecimentoLayout = new javax.swing.GroupLayout(jpReconhecimento);
        jpReconhecimento.setLayout(jpReconhecimentoLayout);
        jpReconhecimentoLayout.setHorizontalGroup(
            jpReconhecimentoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 227, Short.MAX_VALUE)
        );
        jpReconhecimentoLayout.setVerticalGroup(
            jpReconhecimentoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 562, Short.MAX_VALUE)
        );

        jtbMain.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);

        jbApagar2.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jbApagar2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/cancel_icon.png"))); // NOI18N
        jbApagar2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb20.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb20.setText("0");
        jb20.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jbOkAluno.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jbOkAluno.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/ok_icon.png"))); // NOI18N
        jbOkAluno.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jbOkAluno.setMaximumSize(new java.awt.Dimension(45, 41));
        jbOkAluno.setMinimumSize(new java.awt.Dimension(45, 41));
        jbOkAluno.setPreferredSize(new java.awt.Dimension(45, 41));
        jbOkAluno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbOkAlunoActionPerformed(evt);
            }
        });

        jb21.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb21.setText("9");
        jb21.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb21ActionPerformed(evt);
            }
        });

        jb22.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb22.setText("8");
        jb22.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb23.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb23.setText("7");
        jb23.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb24.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb24.setText("4");
        jb24.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb25.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb25.setText("5");
        jb25.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb26.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb26.setText("6");
        jb26.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb26.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb26ActionPerformed(evt);
            }
        });

        jb27.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb27.setText("3");
        jb27.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb27.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb27ActionPerformed(evt);
            }
        });

        jb28.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb28.setText("2");
        jb28.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb29.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb29.setText("1");
        jb29.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 28)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("CADASTRE-SE AQUI");

        jLabel7.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Insira seu RG de Aluno");
        jLabel7.setToolTipText("");

        jftRGAluno.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jftRGAluno.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(162, 162, 162)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jb29)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jb28))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jb24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jb25))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jbApagar2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(jb23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jb20)
                            .addComponent(jb22))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jb21)
                    .addComponent(jbOkAluno, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jb26)
                    .addComponent(jb27))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jftRGAluno)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 454, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(150, 150, 150)
                .addComponent(jLabel6)
                .addGap(18, 18, 18)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jftRGAluno, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb29, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb28, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb27, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb25, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb21, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jbOkAluno, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jbApagar2))
                    .addComponent(jb20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(200, Short.MAX_VALUE))
        );

        jtbMain.addTab("tab2", jPanel1);

        jpInfos.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 5));

        jlBemvindo.setFont(new java.awt.Font("Tahoma", 0, 36)); // NOI18N
        jlBemvindo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jlBemvindo.setText("Olá, Pessoa!");
        jlBemvindo.setPreferredSize(new java.awt.Dimension(102, 42));

        jLabelAula.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabelAula.setText("Disciplina:");
        jLabelAula.setPreferredSize(new java.awt.Dimension(25, 17));

        jlDisciplina.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jlDisciplina.setText("Projeto Integrador III");

        jLabelSala.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabelSala.setText("Sala:");
        jLabelSala.setPreferredSize(new java.awt.Dimension(25, 17));

        jlSala.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jlSala.setText("DCEENG 214");

        jlDescricao.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jlDescricao.setText("Suas atividades de hoje, são as descritas abaixo:");

        javax.swing.GroupLayout jpInfosLayout = new javax.swing.GroupLayout(jpInfos);
        jpInfos.setLayout(jpInfosLayout);
        jpInfosLayout.setHorizontalGroup(
            jpInfosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpInfosLayout.createSequentialGroup()
                .addGap(120, 120, 120)
                .addComponent(jlBemvindo, javax.swing.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE)
                .addGap(120, 120, 120))
            .addGroup(jpInfosLayout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(jpInfosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jlDescricao)
                    .addGroup(jpInfosLayout.createSequentialGroup()
                        .addComponent(jLabelSala, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jlSala, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpInfosLayout.createSequentialGroup()
                        .addComponent(jLabelAula, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jlDisciplina, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jpInfosLayout.setVerticalGroup(
            jpInfosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpInfosLayout.createSequentialGroup()
                .addGap(180, 180, 180)
                .addComponent(jlBemvindo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40)
                .addComponent(jlDescricao)
                .addGap(18, 18, 18)
                .addGroup(jpInfosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelAula, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jlDisciplina, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(jpInfosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelSala, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jlSala, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(329, Short.MAX_VALUE))
        );

        jtbMain.addTab("tab3", jpInfos);

        jpCadastrar1.setForeground(new java.awt.Color(255, 255, 255));

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 28)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("CADASTRE-SE AQUI");

        jLabel5.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("Insira seu CPF");
        jLabel5.setToolTipText("");

        jftCPF1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jftCPF1.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N

        jb10.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb10.setText("1");
        jb10.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb11.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb11.setText("2");
        jb11.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb12.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb12.setText("3");
        jb12.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb12ActionPerformed(evt);
            }
        });

        jb13.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb13.setText("4");
        jb13.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb14.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb14.setText("5");
        jb14.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb15.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb15.setText("6");
        jb15.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb15ActionPerformed(evt);
            }
        });

        jb16.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb16.setText("7");
        jb16.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb17.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb17.setText("8");
        jb17.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jb18.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb18.setText("9");
        jb18.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb18ActionPerformed(evt);
            }
        });

        jbOk1.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jbOk1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/ok_icon.png"))); // NOI18N
        jbOk1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jbOk1.setMaximumSize(new java.awt.Dimension(45, 41));
        jbOk1.setMinimumSize(new java.awt.Dimension(45, 41));
        jbOk1.setPreferredSize(new java.awt.Dimension(45, 41));
        jbOk1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbOk1ActionPerformed(evt);
            }
        });

        jb19.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb19.setText("0");
        jb19.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jbApagar1.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jbApagar1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/cancel_icon.png"))); // NOI18N
        jbApagar1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jpCadastrar1Layout = new javax.swing.GroupLayout(jpCadastrar1);
        jpCadastrar1.setLayout(jpCadastrar1Layout);
        jpCadastrar1Layout.setHorizontalGroup(
            jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpCadastrar1Layout.createSequentialGroup()
                .addGap(162, 162, 162)
                .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpCadastrar1Layout.createSequentialGroup()
                        .addComponent(jb10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jb11))
                    .addGroup(jpCadastrar1Layout.createSequentialGroup()
                        .addComponent(jb13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jb14))
                    .addGroup(jpCadastrar1Layout.createSequentialGroup()
                        .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jbApagar1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(jb16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jb19)
                            .addComponent(jb17))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jb18)
                    .addComponent(jbOk1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jb15)
                    .addComponent(jb12))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jpCadastrar1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jftCPF1)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 454, Short.MAX_VALUE))
                .addContainerGap())
        );
        jpCadastrar1Layout.setVerticalGroup(
            jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpCadastrar1Layout.createSequentialGroup()
                .addGap(150, 150, 150)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jftCPF1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(12, 12, 12)
                .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jbOk1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jbApagar1))
                    .addComponent(jb19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(202, Short.MAX_VALUE))
        );

        jtbMain.addTab("tab4", jpCadastrar1);

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jLabel3.setText("Aprovação");
        jLabel3.setToolTipText("");
        jLabel3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jLabel8.setText("Cadastro");
        jLabel8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jLabel9.setText("Aguardando");
        jLabel9.setToolTipText("");
        jLabel9.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap(44, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 420, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 420, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                    .addContainerGap(41, Short.MAX_VALUE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 420, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(13, 13, 13)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(162, 162, 162)
                .addComponent(jLabel9)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addContainerGap(425, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addGap(95, 95, 95)
                    .addComponent(jLabel8)
                    .addContainerGap(557, Short.MAX_VALUE)))
        );

        jtbMain.addTab("tab3", jPanel3);

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("foi aprovado.");

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Seu cadastro não");

        jLabel10.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("novamente?");

        jLabel11.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("Quer tentar");

        jbTentarNovamente.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jbTentarNovamente.setText("Sim");
        jbTentarNovamente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbTentarNovamenteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, 454, Short.MAX_VALUE)
                    .addComponent(jbTentarNovamente, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 454, Short.MAX_VALUE)
                    .addContainerGap()))
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, 454, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(102, 102, 102)
                .addComponent(jLabel1)
                .addGap(103, 103, 103)
                .addComponent(jLabel10)
                .addGap(66, 66, 66)
                .addComponent(jbTentarNovamente, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(290, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addGap(52, 52, 52)
                    .addComponent(jLabel2)
                    .addContainerGap(600, Short.MAX_VALUE)))
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addGap(207, 207, 207)
                    .addComponent(jLabel11)
                    .addContainerGap(445, Short.MAX_VALUE)))
        );

        jtbMain.addTab("tab5", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(96, Short.MAX_VALUE)
                .addComponent(jpReconhecimento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(487, 487, 487)
                .addComponent(jtbMain, javax.swing.GroupLayout.PREFERRED_SIZE, 479, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jpReconhecimento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jtbMain, javax.swing.GroupLayout.DEFAULT_SIZE, 726, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

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

    private void jb12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb12ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jb12ActionPerformed

    private void jb15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb15ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jb15ActionPerformed

    private void jb18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb18ActionPerformed

    }//GEN-LAST:event_jb18ActionPerformed

    private void jbOk1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbOk1ActionPerformed
        programState = programStateRemember;
        stateSave = true;
    }//GEN-LAST:event_jbOk1ActionPerformed

    private void jbOkAlunoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbOkAlunoActionPerformed
        programState = programStateRemember;
        stateSave = true;
        jbOkAlunoPressed = true;
    }//GEN-LAST:event_jbOkAlunoActionPerformed

    private void jb21ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb21ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jb21ActionPerformed

    private void jb26ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb26ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jb26ActionPerformed

    private void jb27ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb27ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jb27ActionPerformed

    private void jbTentarNovamenteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbTentarNovamenteActionPerformed
        jbTentarNovamentePressed = true;
    }//GEN-LAST:event_jbTentarNovamenteActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelAula;
    private javax.swing.JLabel jLabelSala;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JButton jb10;
    private javax.swing.JButton jb11;
    private javax.swing.JButton jb12;
    private javax.swing.JButton jb13;
    private javax.swing.JButton jb14;
    private javax.swing.JButton jb15;
    private javax.swing.JButton jb16;
    private javax.swing.JButton jb17;
    private javax.swing.JButton jb18;
    private javax.swing.JButton jb19;
    private javax.swing.JButton jb20;
    private javax.swing.JButton jb21;
    private javax.swing.JButton jb22;
    private javax.swing.JButton jb23;
    private javax.swing.JButton jb24;
    private javax.swing.JButton jb25;
    private javax.swing.JButton jb26;
    private javax.swing.JButton jb27;
    private javax.swing.JButton jb28;
    private javax.swing.JButton jb29;
    private javax.swing.JButton jbApagar1;
    private javax.swing.JButton jbApagar2;
    private javax.swing.JButton jbOk1;
    private javax.swing.JButton jbOkAluno;
    private javax.swing.JButton jbTentarNovamente;
    private javax.swing.JFormattedTextField jftCPF1;
    private javax.swing.JFormattedTextField jftRGAluno;
    private javax.swing.JLabel jlBemvindo;
    private javax.swing.JLabel jlDescricao;
    private javax.swing.JLabel jlDisciplina;
    private javax.swing.JLabel jlSala;
    private javax.swing.JPanel jpCadastrar1;
    private javax.swing.JPanel jpInfos;
    private javax.swing.JPanel jpReconhecimento;
    private javax.swing.JTabbedPane jtbMain;
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
