package com.tsc.statgui;

import com.siebel.data.SiebelDataBean;
import com.siebel.data.SiebelException;
import com.siebel.data.SiebelPropertySet;
import com.siebel.data.SiebelService;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

interface EndListener {
    void finished();
}


public class SiebelBSExec implements Runnable  {

    private SiebelConnectionData connectionData;
    private BS bs;

    private String method;
    private Map<String, String> inputs;

    private List<EndListener> listeners = new ArrayList<EndListener>();

    public void addListener(EndListener toAdd) {
        listeners.add(toAdd);
    }

    public SiebelBSExec(SiebelConnectionData connectionData, BS bs, String method) {
        this.connectionData = connectionData;
        this.bs = bs;
        this.method = method;
        this.setInputs(this.bs.methods.get(method));
    }
    @Override
    public void run() {
        try {
            SiebelDataBean sblConnect = new SiebelDataBean();
            sblConnect.login("Siebel://" + connectionData.getIp() + ":" + connectionData.getPort() + "/" + connectionData.getEnterprise()
                            + "/" + connectionData.getObjmgr(),
                    connectionData.getUserName(), connectionData.getPassword(), connectionData.getLocale());
            Thread.sleep(50);
            System.out.println("Start BS...");
            SiebelService BS = sblConnect.getService(this.bs.getName());
            SiebelPropertySet Inputs1 = sblConnect.newPropertySet();
            for(Map.Entry<String, String> it: this.inputs.entrySet())
                Inputs1.setProperty(it.getKey(), it.getValue());
            SiebelPropertySet Outputs1 = sblConnect.newPropertySet();
            BS.invokeMethod(this.method, Inputs1, Outputs1);

            sblConnect.logoff();
            System.out.println("BS finished!");
            for (EndListener hl : listeners)
                hl.finished();
        } catch (InterruptedException | SiebelException e) {
            e.printStackTrace();
        }
    }

    public SiebelConnectionData getConnectionData() {
        return connectionData;
    }

    public void setConnectionData(SiebelConnectionData connectionData) {
        this.connectionData = connectionData;
    }

    public BS getBs() {
        return bs;
    }

    public void setBs(BS bs) {
        this.bs = bs;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }


    public Map<String, String> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, String> inputs) {
        this.inputs = inputs;
    }

    public static class BS {
        private String name;
        private Map<String, Map<String, String>> methods;

        public BS(String name, Map<String, Map<String, String>> methods) {
            this.name = name;
            this.methods = methods;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Map<String, String>> getMethods() {
            return methods;
        }

        public void setMethods(Map<String, Map<String, String>> methods) {
            this.methods = methods;
        }
    }

}

