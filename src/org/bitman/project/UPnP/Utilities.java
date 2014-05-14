/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitman.project.UPnP;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author 骜
 */
public class Utilities {

    //Setting for getURL_*
    private static Pattern ipPattern = null;
    private static final String ipRegex = "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\\:([0-9]{1,5})(.*)";
    //Setting for getFreePortUDP
    private static int MINPORT = 30000;
    private static final int MAXPORT = 65000;

    static {
        try {
            ipPattern = Pattern.compile(ipRegex);
        } catch (PatternSyntaxException e) {
        }
    }

    public static String SSDP_GetDescXMLAddress(String text) {
        if (!text.startsWith("HTTP/1.1 200 OK\r\n")) {
            return null;
        }
        Pattern tagRegex = Pattern.compile("^LOCATION: (.+?)$", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagRegex.matcher(text);
        tagMatcher.find();
        try {
            tagMatcher.group(1);
        } catch (Exception ex) {
            return null;
        }
        return tagMatcher.group(1);
    }

    public static String MatchTagGreedy(String text, String tag) {
        Pattern tagRegex = Pattern.compile("<" + tag + ">(.*)</" + tag + ">", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagRegex.matcher(text);
        tagMatcher.find();
        try {
            tagMatcher.group(1);
        } catch (Exception ex) {
            return null;
        }
        return tagMatcher.group(1);
    }

    public static String MatchTagReluctant(String text, String tag) {
        Pattern tagRegex = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagRegex.matcher(text);
        tagMatcher.find();
        try {
            tagMatcher.group(1);
        } catch (Exception ex) {
            return null;
        }
        return tagMatcher.group(1);
    }

    public static String getURL_IpAddress(String URLAddress) {
        Matcher ip_mat = ipPattern.matcher(URLAddress);
        ip_mat.find();
        try {
            return new String(ip_mat.group(1));
        } catch (Exception ex) {
            System.err.println("Wrong format of URL link.");
            return null;
        }
    }

    public static String getURL_PortAddress(String URLAddress) {
        Matcher ip_mat = ipPattern.matcher(URLAddress);
        ip_mat.find();
        try {
            return new String(ip_mat.group(2));
        } catch (Exception ex) {
            System.err.println("Wrong format of URL link.");
            return null;
        }
    }

    public static String getURL_Resource(String URLAddress) {
        Matcher ip_mat = ipPattern.matcher(URLAddress);
        ip_mat.find();
        try {
            return new String(ip_mat.group(3));
        } catch (Exception ex) {
            System.err.println("Wrong format of URL link.");
            return null;
        }
    }

    public static int[] getFreePortUDP(int number) {
        int[] rtn = new int[number];
        int count = 0;
        DatagramSocket s = null;


        while ((count < number) && (MINPORT < MAXPORT)) {
            try {
                s = new DatagramSocket(MINPORT, InetAddress.getByName(GetIP.getLocalIpAddress(true)));
                s.close();
                rtn[count] = MINPORT;
                ++count;
                ++MINPORT;
            } catch (IOException e) {
                ++MINPORT;
                count = 0;
                continue;
            }
        }
        if (MINPORT >= MAXPORT) {
            
            MINPORT = 30000;
            return null;//no port available from MAXPORT to MINPORT
        } else {
            return rtn;
        }
    }
}