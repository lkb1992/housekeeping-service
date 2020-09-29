package com.example.housekeepingservice.utils;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;


public class SftpUtils {
    private static String reservedDirectory;
    private static Boolean flag = true;
    private static Integer fileDeletedCount = 0;
    private Logger logger = LoggerFactory.getLogger(SftpUtils.class);
    /**
     * 连接sftp服务器
     *
     * @param host
     *            主机
     * @param port
     *            端口
     * @param username
     *            用户名
     * @param password
     *            密码
     * @return
     */
    public static ChannelSftp connect(String host, int port, String username, String password) {
        ChannelSftp sftp = null;
        try {
            JSch jsch = new JSch();
            jsch.getSession(username, host, port);
            Session sshSession = jsch.getSession(username, host, port);
            System.out.println("Session created.");
            sshSession.setPassword(password);
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(sshConfig);
            sshSession.connect();
            System.out.println("Session connected.");
            System.out.println("Opening Channel.");
            Channel channel = sshSession.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;
            System.out.println("Connected to " + host + ".");
        } catch (Exception e) {
            e.printStackTrace();
            sftp = null;
        }
        return sftp;
    }

    /**
     *
     * @param directory
     * @param sftp
     * @throws SftpException
     */

    public static void deleteExpiredFile(ChannelSftp sftp,ChannelSftp.LsEntry entry,String directory,Long expiredTime,Boolean switchBotton,Integer fileQunatity) throws SftpException {
        if(null!=entry&&entry.getAttrs().isDir()){
            //进入指定远程目录的子目录
            sftp.cd(directory+entry.getFilename());
        }else{
            //进入指定远程目录
            sftp.cd(directory);
            reservedDirectory = directory.substring(directory.lastIndexOf("/")+1);
        }
        //列出指定目录下所有文件以及文件夹
        Vector totalFiles = sftp.ls(sftp.pwd());
        String currentPwd = sftp.pwd()+"/";
        for (Object object:totalFiles) {
            ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) object;
            String filename = lsEntry.getFilename();
            if (filename.equals(".") || filename.equals("..")) {
                continue;
            }
            SftpATTRS attrs = lsEntry.getAttrs();
            if(!attrs.isDir()){
                Long mTime = Long.valueOf((lsEntry.getAttrs().getMTime()))*1000;
                Long currentTime = System.currentTimeMillis();
                if(currentTime-mTime>expiredTime){
                    //删除过期文件
                    if(flag){
                        sftp.rm(filename);
                    }
                    fileDeletedCount++;
                    if(!switchBotton&&fileDeletedCount==fileQunatity){
                        flag = false;
                    }
                }
            }else{
                //遍历删除子目录下的文件
                deleteExpiredFile(sftp,lsEntry,currentPwd,expiredTime,switchBotton,fileQunatity);
            }
        }
        //删除空文件夹
        deleteRemoteEmptyDir(sftp,currentPwd);
    }

    public static void deleteRemoteEmptyDir(ChannelSftp sftp,String directory) throws SftpException {
        sftp.cd(directory);
        Vector vector = sftp.ls(sftp.pwd());
        String currentPwd = sftp.pwd();
        String deleteDir = currentPwd.substring(currentPwd.lastIndexOf("/")+1);
        if(vector.size()==2&&!deleteDir.equals(reservedDirectory)){//空文件
            sftp.cd("../");
            sftp.rmdir(deleteDir);
        }else{//遍历空文件
            for (Object o :vector){
                ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) o;
                String filename = lsEntry.getFilename();
                if (filename.equals(".") || filename.equals("..")) {
                    continue;
                }
                if (lsEntry.getAttrs().isDir()){
                    deleteRemoteEmptyDir(sftp,currentPwd+"/"+filename);
                }
            }
        }
    }
}