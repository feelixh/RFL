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
import java.util.Calendar;
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
    boolean jbOKUpdatePressed = false;
    boolean jbNaoSouALunoPressed = false;
    boolean jbSouALunoPressed = false;
    private boolean stateSave = false;
    private long lastIdDetected = -1;
    private HashMap<String, String> currentUser = null;
    private HashMap<String, String> updateUser = null;
    private String codClass[][] = new String[10][2];
    private boolean jbTentarNovamentePressed = false;
    Random rand;
    Calendar calendar;

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
                    jtbMain.setBounds((getWidth() / 2), (getHeight() - Math.round((float) getHeight() / 1.5f)) / 2, (getWidth() / 2) - 20, Math.round((float) getHeight() / 1.5f));
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
                            setPanelNoFace();

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
                                                updateUser = currentUser;
                                                updateUser.put("ativo", "4");
                                                // removeUser(IDs[i], "img/" + name[0].substring(name[0].indexOf("-") + 1, name[0].length()));
                                                //FSDK.PurgeID(tracker, IDs[i]);
                                                //  saveTracker();
                                                setPanelUpdateRG();
                                            }
                                        } else if (Integer.parseInt(currentUser.get("ativo")) == 0) {
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
                                if (jbNaoSouALunoPressed) {
                                    setPanelCadastroCPF();
                                } else if (jbSouALunoPressed) {
                                    setPanelCadastroRG();
                                }
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
                                if (programStateRemember == programState && jbOKUpdatePressed == true) {
                                    if (FSDK.FSDKE_OK == FSDK.LockID(tracker, IDs[i])) {
                                        jbOKUpdatePressed = false;
                                        userName = Long.toString(IDs[i]) + "-" + getRGUpdate();
                                        FSDK.SetName(tracker, IDs[i], userName);
                                        FSDK.UnlockID(tracker, IDs[i]);
                                        System.out.println("tr " + tracker);
                                        System.out.println("id " + IDs[i]);
                                        FSDK.SaveImageToFile(imageHandle, "img/" + getRGUpdate() + ".jpg");
                                        updateUser(IDs[i], getRGUpdate(), "img/" + getRGUpdate() + ".jpg", updateUser.get("id_reconhecimento"), updateUser.get("rg_aluno"));
                                        saveTracker();
                                        currentUser.put("ativo", "0");
                                        System.out.println("salvooooooo sa porra");

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

    private String getRGUpdate() {
        if (jftRGAlunoUpdate.getText().length() > 0) {
            return jftRGAlunoUpdate.getText();

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

    private boolean updateUser(long id, String cpf, String pathImagem, String id_banco, String old_RG) {
        try {

            String sql = "update reconhecimento set ativo=0, rg_aluno = '" + cpf + "' , tracker = '" + Long.toString(id) + "', imagem ='" + pathImagem + "' where id = " + id_banco + " and rg_aluno = '" + old_RG + "';";
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
            String sql = "select r.id as id, r.ativo, u.nome_aluno, u.nome_ativ_curric, r.rg_aluno, substring(cod_turma,position('_' in cod_turma)+1,1) as cod_turma from reconhecimento as r inner join unijui as u on u.matr_aluno = r.rg_aluno where r.tracker = " + Long.toString(id) + " and r.imagem like '" + pathImagem + "%' order by cod_turma;";
            System.out.println(sql);
            if (connection.isClosed()) {
                getConn(url, userBanco, pwBanco);
                resultSet = statement.executeQuery(sql);
                int i = 0;
                while (resultSet.next()) {
                    codClass[i][0] = resultSet.getString("cod_turma");
                    codClass[i][1] = resultSet.getString("nome_ativ_curric");
                    i++;
                }

                for (int z = 0; z < codClass.length; z++) {
                    if (codClass[z][0] != null) {
                        System.out.println(codClass[z][0] + " - " + codClass[z][1]);
                        if (Integer.parseInt(codClass[z][0]) == getDayWeek()) {
                            userInfo.put("materia", codClass[z][1]);
                            break;
                        }
                    }

                }

                resultSet.last();
                userInfo.put("ativo", resultSet.getString("ativo"));
                userInfo.put("nome", resultSet.getString("nome_aluno"));

                userInfo.put("rg_aluno", resultSet.getString("rg_aluno"));
                userInfo.put("id_reconhecimento", resultSet.getString("id"));
                return userInfo;

            } else {
                resultSet = statement.executeQuery(sql);
                int i = 0;
                while (resultSet.next()) {
                    codClass[i][0] = resultSet.getString("cod_turma");
                    codClass[i][1] = resultSet.getString("nome_ativ_curric");
                    i++;
                }

                for (int z = 0; z < codClass.length; z++) {
                    if (codClass[z][0] != null) {
                        System.out.println(codClass[z][0] + " - " + codClass[z][1]);
                        if (Integer.parseInt(codClass[z][0]) == getDayWeek()) {
                            userInfo.put("materia", codClass[z][1]);
                            break;
                        }
                    }

                }

                resultSet.last();
                userInfo.put("ativo", resultSet.getString("ativo"));
                userInfo.put("nome", resultSet.getString("nome_aluno"));

                userInfo.put("rg_aluno", resultSet.getString("rg_aluno"));
                userInfo.put("id_reconhecimento", resultSet.getString("id"));
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

    private void setPanelUpdateRG() {
        jtbMain.setSelectedIndex(5);
    }

    private void setPanelNoFace() {
        jtbMain.setSelectedIndex(6);
    }

    private void setPanelCadastroCPF() {
        jtbMain.setSelectedIndex(2);
    }

    private int getDayWeek() {
        calendar = Calendar.getInstance();
        return 6;
        //return calendar.get(Calendar.DAY_OF_WEEK);
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
        jbNaoSouAluno = new javax.swing.JButton();
        jpInfos = new javax.swing.JPanel();
        jlBemvindo = new javax.swing.JLabel();
        jLabelAula = new javax.swing.JLabel();
        jlDisciplina = new javax.swing.JLabel();
        jLabelSala = new javax.swing.JLabel();
        jlSala = new javax.swing.JLabel();
        jlDescricao = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
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
        jbNaoSouAluno1 = new javax.swing.JButton();
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
        jPanel4 = new javax.swing.JPanel();
        jb30 = new javax.swing.JButton();
        jb31 = new javax.swing.JButton();
        jb32 = new javax.swing.JButton();
        jb33 = new javax.swing.JButton();
        jb34 = new javax.swing.JButton();
        jb35 = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jftRGAlunoUpdate = new javax.swing.JFormattedTextField();
        jbApagar3 = new javax.swing.JButton();
        jb36 = new javax.swing.JButton();
        jbOKAtuaizarAluno = new javax.swing.JButton();
        jb37 = new javax.swing.JButton();
        jb38 = new javax.swing.JButton();
        jb39 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();

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
        jbApagar2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbApagar2ActionPerformed(evt);
            }
        });

        jb20.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb20.setText("0");
        jb20.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb20ActionPerformed(evt);
            }
        });

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
        jb22.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb22ActionPerformed(evt);
            }
        });

        jb23.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb23.setText("7");
        jb23.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb23.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb23ActionPerformed(evt);
            }
        });

        jb24.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb24.setText("4");
        jb24.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb24.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb24ActionPerformed(evt);
            }
        });

        jb25.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb25.setText("5");
        jb25.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb25.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb25ActionPerformed(evt);
            }
        });

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
        jb28.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb28ActionPerformed(evt);
            }
        });

        jb29.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb29.setText("1");
        jb29.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb29.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb29ActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 28)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("CADASTRE-SE AQUI");

        jLabel7.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Insira seu RG de Aluno");
        jLabel7.setToolTipText("");

        jftRGAluno.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jftRGAluno.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N

        jbNaoSouAluno.setText("Não sou aluno?");
        jbNaoSouAluno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbNaoSouAlunoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jftRGAluno)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(150, 150, 150)
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
                                    .addComponent(jb23))
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
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jbNaoSouAluno, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
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
                    .addComponent(jb20))
                .addGap(41, 41, 41)
                .addComponent(jbNaoSouAluno)
                .addGap(272, 272, 272))
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

        jLabel17.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel17.setText("Pauta: Apresentação Final ");

        jLabel18.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel18.setText("Entrega do Produto pronto - go-to-market");

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
                    .addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jpInfosLayout.createSequentialGroup()
                        .addComponent(jLabelAula, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jlDisciplina, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jpInfosLayout.createSequentialGroup()
                        .addComponent(jLabelSala, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jlSala, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jpInfosLayout.createSequentialGroup()
                        .addComponent(jlDescricao)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpInfosLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 366, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel18)
                .addContainerGap(268, Short.MAX_VALUE))
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
        jb10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb10ActionPerformed(evt);
            }
        });

        jb11.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb11.setText("2");
        jb11.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb11ActionPerformed(evt);
            }
        });

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
        jb13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb13ActionPerformed(evt);
            }
        });

        jb14.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb14.setText("5");
        jb14.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb14ActionPerformed(evt);
            }
        });

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
        jb16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb16ActionPerformed(evt);
            }
        });

        jb17.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb17.setText("8");
        jb17.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb17ActionPerformed(evt);
            }
        });

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
        jb19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb19ActionPerformed(evt);
            }
        });

        jbApagar1.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jbApagar1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/cancel_icon.png"))); // NOI18N
        jbApagar1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jbApagar1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbApagar1ActionPerformed(evt);
            }
        });

        jbNaoSouAluno1.setText("Sou aluno?");
        jbNaoSouAluno1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbNaoSouAluno1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpCadastrar1Layout = new javax.swing.GroupLayout(jpCadastrar1);
        jpCadastrar1.setLayout(jpCadastrar1Layout);
        jpCadastrar1Layout.setHorizontalGroup(
            jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpCadastrar1Layout.createSequentialGroup()
                .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpCadastrar1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jftCPF1)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 454, Short.MAX_VALUE)))
                    .addGroup(jpCadastrar1Layout.createSequentialGroup()
                        .addGroup(jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                                    .addComponent(jb12)))
                            .addGroup(jpCadastrar1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jbNaoSouAluno1, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 149, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jpCadastrar1Layout.setVerticalGroup(
            jpCadastrar1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpCadastrar1Layout.createSequentialGroup()
                .addContainerGap()
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
                    .addComponent(jb19))
                .addGap(39, 39, 39)
                .addComponent(jbNaoSouAluno1)
                .addGap(275, 275, 275))
        );

        jtbMain.addTab("tab4", jpCadastrar1);

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Aprovação");
        jLabel3.setToolTipText("");
        jLabel3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("Cadastro");
        jLabel8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 36)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
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
                .addContainerGap(291, Short.MAX_VALUE))
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

        jb30.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb30.setText("4");
        jb30.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb30.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb30ActionPerformed(evt);
            }
        });

        jb31.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb31.setText("5");
        jb31.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb31.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb31ActionPerformed(evt);
            }
        });

        jb32.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb32.setText("6");
        jb32.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb32.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb32ActionPerformed(evt);
            }
        });

        jb33.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb33.setText("3");
        jb33.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb33.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb33ActionPerformed(evt);
            }
        });

        jb34.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb34.setText("2");
        jb34.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb34.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb34ActionPerformed(evt);
            }
        });

        jb35.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb35.setText("1");
        jb35.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb35.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb35ActionPerformed(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("Tahoma", 0, 28)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("ATUALIZE SEU CADASTRO AQUI");

        jLabel13.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel13.setText("Insira seu RG de Aluno");
        jLabel13.setToolTipText("");

        jftRGAlunoUpdate.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jftRGAlunoUpdate.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N

        jbApagar3.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jbApagar3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/cancel_icon.png"))); // NOI18N
        jbApagar3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jbApagar3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbApagar3ActionPerformed(evt);
            }
        });

        jb36.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb36.setText("0");
        jb36.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb36.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb36ActionPerformed(evt);
            }
        });

        jbOKAtuaizarAluno.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jbOKAtuaizarAluno.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/ok_icon.png"))); // NOI18N
        jbOKAtuaizarAluno.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jbOKAtuaizarAluno.setMaximumSize(new java.awt.Dimension(45, 41));
        jbOKAtuaizarAluno.setMinimumSize(new java.awt.Dimension(45, 41));
        jbOKAtuaizarAluno.setPreferredSize(new java.awt.Dimension(45, 41));
        jbOKAtuaizarAluno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbOKAtuaizarAlunoActionPerformed(evt);
            }
        });

        jb37.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb37.setText("9");
        jb37.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb37.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb37ActionPerformed(evt);
            }
        });

        jb38.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb38.setText("8");
        jb38.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb38.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb38ActionPerformed(evt);
            }
        });

        jb39.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        jb39.setText("7");
        jb39.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jb39.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb39ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(162, 162, 162)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jb35)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jb34))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jb30)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jb31))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jbApagar3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(jb39, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jb36)
                            .addComponent(jb38))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jb37)
                    .addComponent(jbOKAtuaizarAluno, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jb32)
                    .addComponent(jb33))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jftRGAlunoUpdate)
                    .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(150, 150, 150)
                .addComponent(jLabel12)
                .addGap(18, 18, 18)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jftRGAlunoUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb35, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb34, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb33, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb30, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb31, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb32, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(12, 12, 12)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jb39, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb38, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jb37, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jbOKAtuaizarAluno, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jbApagar3))
                    .addComponent(jb36, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(200, Short.MAX_VALUE))
        );

        jtbMain.addTab("tab6", jPanel4);

        jLabel14.setFont(new java.awt.Font("Tahoma", 0, 22)); // NOI18N
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel14.setText("Reconhecimento Facial ");
        jLabel14.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel15.setFont(new java.awt.Font("Tahoma", 0, 22)); // NOI18N
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setText("UNIJUÍ ");
        jLabel15.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel16.setFont(new java.awt.Font("Tahoma", 0, 22)); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel16.setText("Apoxime-se!");
        jLabel16.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                    .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(73, 73, 73)
                .addComponent(jLabel14)
                .addGap(18, 18, 18)
                .addComponent(jLabel15)
                .addGap(18, 18, 18)
                .addComponent(jLabel16)
                .addContainerGap(509, Short.MAX_VALUE))
        );

        jtbMain.addTab("tab7", jPanel5);

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
        addDigitF(jftCPF1, 3);
    }//GEN-LAST:event_jb12ActionPerformed

    private void jb15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb15ActionPerformed
        addDigitF(jftCPF1, 6);
    }//GEN-LAST:event_jb15ActionPerformed

    private void jb18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb18ActionPerformed
        addDigitF(jftCPF1, 9);
    }//GEN-LAST:event_jb18ActionPerformed

    private void jbOk1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbOk1ActionPerformed

    }//GEN-LAST:event_jbOk1ActionPerformed

    private void jbOkAlunoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbOkAlunoActionPerformed
        programState = programStateRemember;
        stateSave = true;
        jbOkAlunoPressed = true;
    }//GEN-LAST:event_jbOkAlunoActionPerformed

    private void jb21ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb21ActionPerformed
        addDigit(jftRGAluno, 9);
    }//GEN-LAST:event_jb21ActionPerformed

    private void jb26ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb26ActionPerformed
        addDigit(jftRGAluno, 6);
    }//GEN-LAST:event_jb26ActionPerformed

    private void jb27ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb27ActionPerformed
        addDigit(jftRGAluno, 3);
    }//GEN-LAST:event_jb27ActionPerformed

    private void jbTentarNovamenteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbTentarNovamenteActionPerformed
        jbTentarNovamentePressed = true;
    }//GEN-LAST:event_jbTentarNovamenteActionPerformed

    private void jb32ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb32ActionPerformed
        addDigit(jftRGAlunoUpdate, 6);
    }//GEN-LAST:event_jb32ActionPerformed

    private void jb33ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb33ActionPerformed
        addDigit(jftRGAlunoUpdate, 3);
    }//GEN-LAST:event_jb33ActionPerformed

    private void jbOKAtuaizarAlunoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbOKAtuaizarAlunoActionPerformed
        programState = programStateRemember;
        stateSave = true;
        jbOKUpdatePressed = true;
    }//GEN-LAST:event_jbOKAtuaizarAlunoActionPerformed

    private void jb37ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb37ActionPerformed
        addDigit(jftRGAlunoUpdate, 9);
    }//GEN-LAST:event_jb37ActionPerformed

    private void jb29ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb29ActionPerformed
        addDigit(jftRGAluno, 1);
    }//GEN-LAST:event_jb29ActionPerformed

    private void jb28ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb28ActionPerformed
        addDigit(jftRGAluno, 2);
    }//GEN-LAST:event_jb28ActionPerformed

    private void jb24ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb24ActionPerformed
        addDigit(jftRGAluno, 4);
    }//GEN-LAST:event_jb24ActionPerformed

    private void jb25ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb25ActionPerformed
        addDigit(jftRGAluno, 5);
    }//GEN-LAST:event_jb25ActionPerformed

    private void jb23ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb23ActionPerformed
        addDigit(jftRGAluno, 7);
    }//GEN-LAST:event_jb23ActionPerformed

    private void jb22ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb22ActionPerformed
        addDigit(jftRGAluno, 8);
    }//GEN-LAST:event_jb22ActionPerformed

    private void jb20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb20ActionPerformed
        addDigit(jftRGAluno, 0);
    }//GEN-LAST:event_jb20ActionPerformed

    private void jbApagar2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbApagar2ActionPerformed
        clearText(jftRGAluno);
    }//GEN-LAST:event_jbApagar2ActionPerformed

    private void jb35ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb35ActionPerformed
        addDigit(jftRGAlunoUpdate, 1);
    }//GEN-LAST:event_jb35ActionPerformed

    private void jb34ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb34ActionPerformed
        addDigit(jftRGAlunoUpdate, 2);
    }//GEN-LAST:event_jb34ActionPerformed

    private void jb30ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb30ActionPerformed
        addDigit(jftRGAlunoUpdate, 4);
    }//GEN-LAST:event_jb30ActionPerformed

    private void jb31ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb31ActionPerformed
        addDigit(jftRGAlunoUpdate, 5);
    }//GEN-LAST:event_jb31ActionPerformed

    private void jb39ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb39ActionPerformed
        addDigit(jftRGAlunoUpdate, 7);
    }//GEN-LAST:event_jb39ActionPerformed

    private void jb38ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb38ActionPerformed
        addDigit(jftRGAlunoUpdate, 8);
    }//GEN-LAST:event_jb38ActionPerformed

    private void jb36ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb36ActionPerformed
        addDigit(jftRGAlunoUpdate, 0);
    }//GEN-LAST:event_jb36ActionPerformed

    private void jbApagar3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbApagar3ActionPerformed
        clearText(jftRGAlunoUpdate);
    }//GEN-LAST:event_jbApagar3ActionPerformed

    private void jbNaoSouAlunoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbNaoSouAlunoActionPerformed
        jbSouALunoPressed = false;
        jbNaoSouALunoPressed = true;
    }//GEN-LAST:event_jbNaoSouAlunoActionPerformed

    private void jbNaoSouAluno1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbNaoSouAluno1ActionPerformed
        jbNaoSouALunoPressed = false;
        jbSouALunoPressed = true;
    }//GEN-LAST:event_jbNaoSouAluno1ActionPerformed

    private void jb10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb10ActionPerformed
        addDigitF(jftCPF1, 1);
    }//GEN-LAST:event_jb10ActionPerformed

    private void jb11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb11ActionPerformed
        addDigitF(jftCPF1, 2);
    }//GEN-LAST:event_jb11ActionPerformed

    private void jb13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb13ActionPerformed
        addDigitF(jftCPF1, 4);
    }//GEN-LAST:event_jb13ActionPerformed

    private void jb14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb14ActionPerformed
        addDigitF(jftCPF1, 5);
    }//GEN-LAST:event_jb14ActionPerformed

    private void jb16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb16ActionPerformed
        addDigitF(jftCPF1, 7);
    }//GEN-LAST:event_jb16ActionPerformed

    private void jb17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb17ActionPerformed
        addDigitF(jftCPF1, 8);
    }//GEN-LAST:event_jb17ActionPerformed

    private void jb19ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jb19ActionPerformed
        addDigitF(jftCPF1, 0);
    }//GEN-LAST:event_jb19ActionPerformed

    private void jbApagar1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbApagar1ActionPerformed
        clearText(jftCPF1);
    }//GEN-LAST:event_jbApagar1ActionPerformed
    private void addDigit(JFormattedTextField t, int number) {
        t.setText(t.getText() + Integer.toString(number));
    }

    private void addDigitF(JFormattedTextField t, int number) {
        t.setText(t.getText().replaceAll("\\D+", "") + Integer.toString(number));
    }

    private void clearText(JFormattedTextField t) {
        t.setText("");
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
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
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
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
    private javax.swing.JButton jb30;
    private javax.swing.JButton jb31;
    private javax.swing.JButton jb32;
    private javax.swing.JButton jb33;
    private javax.swing.JButton jb34;
    private javax.swing.JButton jb35;
    private javax.swing.JButton jb36;
    private javax.swing.JButton jb37;
    private javax.swing.JButton jb38;
    private javax.swing.JButton jb39;
    private javax.swing.JButton jbApagar1;
    private javax.swing.JButton jbApagar2;
    private javax.swing.JButton jbApagar3;
    private javax.swing.JButton jbNaoSouAluno;
    private javax.swing.JButton jbNaoSouAluno1;
    private javax.swing.JButton jbOKAtuaizarAluno;
    private javax.swing.JButton jbOk1;
    private javax.swing.JButton jbOkAluno;
    private javax.swing.JButton jbTentarNovamente;
    private javax.swing.JFormattedTextField jftCPF1;
    private javax.swing.JFormattedTextField jftRGAluno;
    private javax.swing.JFormattedTextField jftRGAlunoUpdate;
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
