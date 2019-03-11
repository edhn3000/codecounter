package com.edhn.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * PropReader
 * load prop files, the prop later can override the same key prop before 
 * @author fengyq
 *
 */
public class PropReader {
    
    /**    props files or resources     */
    private Map<String, Hashtable<Object,Object>> props = 
            new LinkedHashMap<String, Hashtable<Object,Object>>();
    
    private static PropReader instance = new PropReader();

    /**
     * @return the instance
     */
    public static PropReader getInstance() {
        return instance;
    }

    /**
     * @param f file
     * @param allowEscape allow escape char in prop file
     * @param charSet charset
     * @return
     * @throws IOException
     */
    public boolean loadPropFile(File f, boolean allowEscape, String charSet) throws IOException {
        InputStream is = null;
        try {
            String fileName = f.getAbsolutePath();
            Hashtable<Object,Object> prop = props.get(fileName);
            if (prop == null) {
                if (!f.exists())
                    return false;
                is = new FileInputStream(f);
                loadPropStream(fileName, is, allowEscape, charSet);
            }
            return true;
        } finally {
            if (is != null)
                is.close();
        }
    }
    

    /**
     * @param f file
     * @param allowEscape allow escape char in prop file
     * @return
     * @throws IOException
     */
    public boolean loadPropFile(File f, boolean allowEscape) throws IOException {
        return loadPropFile(f, allowEscape, null);
    }
    
    /**
     * @param resourcePath
     * @param allowEscape
     * @return
     * @throws IOException
     */
    public boolean loadPropResource(String resourcePath, boolean allowEscape, String charSet) throws IOException {
        InputStream is = null;
        try {
            Hashtable<Object,Object> prop = this.props.get(resourcePath);
            if (prop == null) {
                is = PropReader.class.getResourceAsStream(resourcePath);
                if (is == null) {
                    return false;
                }
                loadPropStream(resourcePath, is, allowEscape, charSet);
            }
            return true;
        } finally {
            if (is != null)
                is.close();
        }
    }

    /**
     * @param resourcePath resourcePath
     * @return
     * @throws IOException
     */
    public boolean loadPropResource(String resourcePath, boolean allowEscape) throws IOException {
        return loadPropResource(resourcePath, allowEscape, null);
    }

    /**
     * @param is is
     * @throws IOException 
     */
    private Hashtable<Object, Object> loadPropStream(String propName,
            InputStream is, boolean allowEscape, String charSet) throws IOException {
        Hashtable<Object, Object> p = null;
        if (allowEscape) {
            Properties prop = new Properties();
            if (charSet == null) {
                prop.load(is);
            } else {
                InputStreamReader reader = new InputStreamReader(is, charSet);
                prop.load(reader);
                reader.close();
            }
            p = prop;
        } else {
            p = loadStreamNoEscape(propName, is, charSet);
        }
        this.props.put(propName, p);
        return p;
    }
    
    /**
     * @param propKey
     * @param is
     * @param charSet
     * @return
     * @throws IOException
     */
    private Hashtable<Object, Object> loadStreamNoEscape(String propKey,
            InputStream is, String charSet) throws IOException {
        BufferedReader reader = null;
        if (charSet != null) {
            reader = new BufferedReader(new InputStreamReader(is, charSet));
        } else {
            reader = new BufferedReader(new InputStreamReader(is));
        }
        try {
            Hashtable<Object, Object> p = new Hashtable<Object, Object>();
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("#") || line.startsWith("[")) {
                    line = reader.readLine();
                    continue;
                }
                int pos = line.indexOf("=");
                if (pos > 0) {
                    String key = line.substring(0, pos).trim();
                    String value = line.substring(pos + 1).trim();
                    p.put(key, value);
                }
                line = reader.readLine();
            }
            this.props.put(propKey, p);
            return p;
        } finally {
            if (reader != null)
                reader.close();
        }
    }
    

    /**
     * @param key key
     * @return
     */
    public String getPropValue(String key) {
        return getPropValue(key, null);
    }

    /**
     * getPropValue
     * @param key key
     * @param def def
     * @return
     */
    public String getPropValue(String key, String def) {
        String value = null;
        Iterator<Entry<String, Hashtable<Object, Object>>> it = props.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Hashtable<Object, Object>> en = it.next();
            Hashtable<Object, Object> prop = en.getValue();
            if (null == value || "".equals(value)) {
                if (prop.containsKey(key))
                    value = String.valueOf(prop.get(key));
            }
        }
        return value;
    }

    /**
     * getPropIntValue
     * @param key key
     * @param def def
     * @return
     */
    public int getPropIntValue(String key, int def) {
        String value = getPropValue(key);
        int iResult;
        try {
            iResult = Integer.parseInt(value);
        } catch (Exception e) {
            iResult = def;
        }
        return iResult;
    }
    
    /**
     * @return
     */
    public List<String> getPropKeys() {
        List<String> keys = new ArrayList<String>();
        Iterator<Entry<String, Hashtable<Object, Object>>> it = props.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Hashtable<Object, Object>> en = it.next();
            Hashtable<Object, Object> prop = en.getValue();
            Enumeration<Object> propKeys = prop.keys();
            while (propKeys.hasMoreElements()) {
                String key = String.valueOf(propKeys.nextElement());
                if (!keys.contains(key))
                    keys.add(key);
            }
        }
        return keys;
    }

}
