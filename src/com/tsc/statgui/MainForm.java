package com.tsc.statgui;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.JOptionPane;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MainForm extends JFrame {
    private JButton bStart;
    private JButton bKillSession;
    private JButton bStartBS;
    private JButton bGetAll;
    private JPanel rootPanel;
    private JTextField tfFreq;
    private JButton bSetFreq;
    private JCheckBox cbDefaultBS;
    private JComboBox cbListMethods;
    private JLabel lblParams;
    private JButton bPause;

    private JSONObject jobj = new JSONObject();
    private ArrayList<String> pids = null;

    private SiebelConnectionData siebelConnectionData;
    private SshConnectionData sshConnectionData;
    private TCPConnectionData tcpConnectionData;
    private SiebelBSExec.BS bs;
    TCPManager tcpManager;


    public MainForm(String configPath) throws HeadlessException {
        setConfig(configPath);
        setVisible(true);
        setContentPane(rootPanel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(350, 250);
        tfFreq.setText("0,001");
        bGetAll.setEnabled(false);
        bStart.addActionListener(actionEvent -> {
            if (bStart.getText().equals("Старт")) {
                tfFreq.setEnabled(false);
                bKillSession.setEnabled(false);
                bSetFreq.setEnabled(false);
                startAnalyzer();
                bStart.setText("Стоп");
            } else if (bStart.getText().equals("Стоп")) {
                tfFreq.setEnabled(true);
                bKillSession.setEnabled(true);
                bSetFreq.setEnabled(true);
                bGetAll.setEnabled(true);
                try {
                    stopAnalyzer();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (bPause.getText().equals("Продолжить"))
                    bPause.setText("Пауза");
                bStart.setText("Старт");
            }


        });
        bStartBS.addActionListener(actionEvent -> {
            if (!cbDefaultBS.isSelected()) {
                String input = JOptionPane.showInputDialog(null, "Конфиг ВС");
                if (!(input == null || (input != null && ("".equals(input))))) {
                    setBSConfig(input);
                    startBS((String) bs.getMethods().keySet().toArray()[0]);
                }
            } else {
                startBS((String) cbListMethods.getSelectedItem());
                bStartBS.setEnabled(false);
            }
        });
        bSetFreq.addActionListener(actionEvent -> {
            setFreq();
        });
        bKillSession.addActionListener(actionEvent -> {
            for (String pid : this.pids) {
                killSession(pid);
            }

        });
        bGetAll.addActionListener(actionEvent -> {
            for (String str : pids) {
                if (!jobj.containsKey(str)) {
                    JOptionPane.showMessageDialog((Component) actionEvent.getSource(), "Нет инфы");
                } else {
                    openChart(str, jobj);
                }
            }
        });
        cbDefaultBS.addItemListener(itemEvent -> {
            if (!cbDefaultBS.isSelected()) {
                cbListMethods.setEnabled(false);
                lblParams.setText("");
            } else {
                cbListMethods.setEnabled(true);
                displayParams();
            }
        });
        cbListMethods.addActionListener(actionEvent -> {
            displayParams();
        });
        bPause.addActionListener(actionEvent -> {
            if (bPause.getText().equals("Пауза")) {
                pauseAnalyzer();
                bPause.setText("Продолжить");
                tfFreq.setEnabled(true);
                bSetFreq.setEnabled(true);
            } else if (bPause.getText().equals("Продолжить")) {
                continueAnalyzer();
                bPause.setText("Пауза");
                tfFreq.setEnabled(false);
                bSetFreq.setEnabled(false);
            }
        });
    }

    private void displayParams() {
        String method = (String) cbListMethods.getSelectedItem();
        lblParams.setText("<html>Параметры:<br>");
        if (bs.getMethods().containsKey(method)) {
            for (var val : bs.getMethods().get(method).entrySet()) {
                lblParams.setText(lblParams.getText() + val.getKey() + ": " + val.getValue() + "<br>");
            }
        }
        lblParams.setText(lblParams.getText() + "</html>");
    }

    public void setConfig(String file) {

        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(file)) {
            //Read JSON file
            Object obj = jsonParser.parse(reader);

            JSONObject configList = (JSONObject) obj;
            JSONObject connection = (JSONObject) configList.get("connection");
            JSONObject sshConnection = (JSONObject) connection.get("ssh");
            JSONObject siebelConnection = (JSONObject) connection.get("siebel");
            JSONObject tcpConnection = (JSONObject) connection.get("server");
            this.siebelConnectionData = new SiebelConnectionData(siebelConnection.get("ip").toString(),
                    siebelConnection.get("port").toString(),
                    siebelConnection.get("enterprise").toString(),
                    siebelConnection.get("objmgr").toString(),
                    siebelConnection.get("user").toString(),
                    siebelConnection.get("server").toString(),
                    siebelConnection.get("password").toString(),
                    siebelConnection.get("locale").toString());
            this.sshConnectionData = new SshConnectionData(sshConnection.get("ip").toString(),
                    sshConnection.get("port").toString(),
                    sshConnection.get("user").toString(),
                    sshConnection.get("password").toString());
            this.tcpConnectionData = new TCPConnectionData(tcpConnection.get("ip").toString(),
                    tcpConnection.get("port").toString());


            JSONObject bsConfig = (JSONObject) configList.get("BS");
            JSONArray mList = (JSONArray) bsConfig.get("methods");

            Map<String, Map<String, String>> methods = new HashMap<>();
            mList.forEach(method -> {
                JSONObject mObj = (JSONObject) bsConfig.get(method.toString());
                JSONObject inputs = (JSONObject) mObj.get("inputs");

                Map<String, String> inputsMap = new HashMap<>();
                for (Object it : inputs.keySet()) {
                    inputsMap.put(it.toString(), inputs.get(it).toString());
                }
                methods.put(method.toString(), inputsMap);
                cbListMethods.addItem(method.toString());
            });

            this.bs = new SiebelBSExec.BS(bsConfig.get("name").toString(), methods);
            displayParams();
            try {
                this.tcpManager = new TCPManager(this.tcpConnectionData.getHost(), this.tcpConnectionData.getPort());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage());
                System.exit(0);
            }

        } catch (IOException | ParseException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    public void setBSConfig(String str) {
        try {
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(str);
            JSONObject bsConfig = (JSONObject) obj;
            Map<String, Map<String, String>> methods = new HashMap<>();
            for (Object it : bsConfig.keySet()) {
                if (it.toString().equals("name")) continue;
                JSONObject method = (JSONObject) bsConfig.get(it);
                JSONObject inputs = (JSONObject) method.get("inputs");
                Map<String, String> inputsMap = new HashMap<>();
                for (Object it2 : inputs.keySet()) {
                    inputsMap.put(it2.toString(), inputs.get(it2).toString());
                }
                methods.put(it.toString(), inputsMap);
            }
            this.bs = new SiebelBSExec.BS(bsConfig.get("name").toString(), methods);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void setFreq() {
        if (!isNumber(tfFreq.getText().replaceAll(",", "."))) {
            JOptionPane.showMessageDialog(null, "Установите числовое значение!");
        } else {
            try {


                tcpManager.sendCommand("freq:" + tfFreq.getText().replaceAll(",", "."));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        }
    }

    public void startBS(String method) {
        if (!this.bs.getMethods().containsKey(method)) {
            JOptionPane.showMessageDialog((Component) this, "Нет такого метода!");
            return;
        }
        SiebelBSExec bsExec = new SiebelBSExec(this.siebelConnectionData, this.bs, method);
        bsExec.addListener(new EndListener() {
            @Override
            public void finished() {
                bStartBS.setEnabled(true);
            }
        });
        Thread bsT = new Thread(bsExec);
        bsT.start();
    }

    private void startAnalyzer() {
        if (this.pids == null)
            this.pids = findPidsForComp();
        for (String str : this.pids) {
            this.jobj.put(str, new JSONObject());
        }

        String jPids = this.pids.stream().map(Object::toString)
                .collect(Collectors.joining(";"));
        System.out.println(jPids);
        try {
            tcpManager.sendCommand("start:" + jPids);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }

    }

    private void pauseAnalyzer() {
        try {
            tcpManager.sendCommand("pause");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    private void continueAnalyzer() {
        try {
            tcpManager.sendCommand("continue");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    private void stopAnalyzer() throws InterruptedException {
        try {
            tcpManager.sendCommand("stop");
            tcpManager.sendCommand("getresult");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            return;
        }
        String data = tcpManager.getAnswer();
        System.out.println(data);

        Map<String, JSONArray> mems = new HashMap<>();
        for (String str : this.pids) {
            ((JSONObject) this.jobj.get(str)).put("memory", new JSONArray());
        }
        for (String str : data.split(";")) {
            String[] info = str.split(":");
            if (info.length > 1) {
                if (this.jobj.containsKey(info[0])) {
                    ((JSONArray) ((JSONObject) this.jobj.get(info[0])).get("memory")).add(info[1].replaceAll("m", ""));
                }
            }
        }

    }

    public void killSession(String pid) {
        SSHManager instance = new SSHManager(this.sshConnectionData.getUserName(),
                this.sshConnectionData.getPassword(), this.sshConnectionData.getIp(), "");
        String errorMessage = instance.connect();
        if (errorMessage != null) {
            System.err.println(errorMessage);
            return;
        }
        ArrayList<String> res = new ArrayList<>();
        String command = "kill " + pid + "\nexit\n";
        instance.sendCommand(command);
        instance.close();
        this.pids = null;
    }

    public ArrayList<String> findPidsForComp() {
        SSHManager instance = new SSHManager(this.sshConnectionData.getUserName(),
                this.sshConnectionData.getPassword(), this.sshConnectionData.getIp(), "");
        String errorMessage = instance.connect();
        if (errorMessage != null) {
            System.err.println(errorMessage);
            return null;
        }
        ArrayList<String> res = new ArrayList<>();
        String command = ". /u01/app/Siebel/siebsrvr/siebenv.sh\nsrvrmgr /g " + this.siebelConnectionData.getIp() +
                " /e " + this.siebelConnectionData.getEnterprise() + " /u " + this.siebelConnectionData.getUserName() +
                " /p " + this.siebelConnectionData.getPassword() + " /s " + this.siebelConnectionData.getServer() +
                " /c 'list procs for comp " + this.siebelConnectionData.getObjmgr() + " show CC_ALIAS, TK_PID' | grep " +
                this.siebelConnectionData.getObjmgr() + " | awk '{print $2}'\nexit\n";

        instance.sendCommand(command);
        String result = instance.getOutBuff().toString();
        instance.close();

        for (String str : result.replaceAll("\r+", "").split("\n"))
            if (isInteger(str)) res.add(str);
        this.pids = res;
        return res;
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
        return true;
    }

    private boolean isNumber(String s) {
        try {
            Float.parseFloat(s);
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
        return true;
    }

    public static void openChart(String str, JSONObject jobj) {
        try {
            File file = File.createTempFile("tmp", ".html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            String pattern = "<html class=\"wf-roboto-n4-active wf-roboto-n5-active wf-active\"><head>\n" +
                    "  <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n" +
                    "  <script type=\"text/javascript\" charset=\"utf-8\">\n" +
                    "          google.charts.load('current', {\n" +
                    "        'packages': ['corechart']\n" +
                    "      });\n" +
                    "      google.charts.setOnLoadCallback(drawChart);\n" +
                    "\n" +
                    "      function drawChart() {\n" +
                    "\n" +
                    "        var jsn = '" + jobj.get(str).toString() + "';\n" +
                    "      var obj = JSON.parse(jsn);\n" +
                    "\n" +
                    "      var data = new google.visualization.DataTable();\n" +
                    "      data.addColumn('number', \"Выборка\");\n" +
                    "      data.addColumn('number', 'Память после запуска');\n" +
                    "\n" +
                    "      var dataSet = [];\n" +
                    "        \n" +
                    "\n" +
                    "        for( i = 0; i< obj.memory.length; i ++)\n" +
                    "        {\n" +
                    "          dataSet[i] = [i, parseFloat(obj.memory[i])];\n" +
                    "        }\n" +
                    "        data.addRows(dataSet);\n" +
                    "\n" +
                    "        var options = {\n" +
                    "          title: \"Процесс\",\n" +
                    "          subtitle: obj.pid,\n" +
                    "          hAxis: {\n" +
                    "            title: 'Выборка',\n" +
                    "            titleTextStyle: {\n" +
                    "              color: '#333'\n" +
                    "            },\n" +
                    "            slantedText: true,\n" +
                    "            slantedTextAngle: 180\n" +
                    "          },\n" +
                    "          explorer: {\n" +
                    "            axis: 'horizontal',\n" +
                    "            keepInBounds: true,\n" +
                    "            maxZoomIn: 160.0\n" +
                    "          },\n" +
                    "        };\n" +
                    "\n" +
                    "        var chart = new google.visualization.LineChart(document.getElementById('chart_div'));\n" +
                    "        chart.draw(data, options);\n" +
                    "      }\n" +
                    "  </script>\n" +
                    "<body>\n" +
                    "<div id=\"chart_div\" style=\"width: 1000px; height: 800px;\"></div>\n" +
                    "</body>\n" +
                    "</html>";
            writer.write(pattern);
            writer.close();
            Desktop.getDesktop().open(file.getAbsoluteFile());
            file.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(11, 3, new Insets(0, 0, 0, 0), -1, -1));
        bKillSession = new JButton();
        bKillSession.setText("Убить сессии");
        rootPanel.add(bKillSession, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bGetAll = new JButton();
        bGetAll.setText("Получить график");
        rootPanel.add(bGetAll, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bStartBS = new JButton();
        bStartBS.setText("Старт БС");
        rootPanel.add(bStartBS, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bSetFreq = new JButton();
        bSetFreq.setText("Установить частоту");
        rootPanel.add(bSetFreq, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tfFreq = new JTextField();
        rootPanel.add(tfFreq, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        cbDefaultBS = new JCheckBox();
        cbDefaultBS.setSelected(true);
        cbDefaultBS.setText("Использовать БС из конфига");
        rootPanel.add(cbDefaultBS, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        rootPanel.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 4, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        cbListMethods = new JComboBox();
        rootPanel.add(cbListMethods, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Запускаемый метод:");
        rootPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        rootPanel.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 4, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        lblParams = new JLabel();
        lblParams.setText("Label");
        rootPanel.add(lblParams, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        bPause = new JButton();
        bPause.setText("Пауза");
        panel1.add(bPause, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bStart = new JButton();
        bStart.setText("Старт");
        panel1.add(bStart, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        rootPanel.add(separator1, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
