package com.example.housekeepingservice.scheduledtasks;

import com.example.housekeepingservice.utils.SftpUtils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@PropertySource("classpath:housekeeping.properties")
@ConfigurationProperties(prefix = "housekeeping")
public class CronDeleteExpiredFile extends QuartzJobBean {

    @Value("${housekeeping.local-directpry}")
    private String localDirectory;

    private String localRootFilePath;

    @Value("${housekeeping.expired-time}")
    private Long expiredTime;
    @Value("${housekeeping.file-qunatity}")
    private Integer fileQunatity;
    @Value("${housekeeping.switch-botton}")
    private Boolean switchBotton;

    @Value("${housekeeping.remote-directory}")
    private String remoteDirectory;
    @Value("${housekeeping.remote-user}")
    private String remoteUser;
    @Value("${housekeeping.remote-password}")
    private String remotePassword;
    @Value("${housekeeping.remote-ip}")
    private String remoteIp;
    @Value("${housekeeping.remote-port}")
    private Integer remotePort;


    private Long currentTime = 0L;
    private Integer fileDeletedCount = 0;
    private Boolean flag = true;
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        localRootFilePath = localDirectory.substring(localDirectory.lastIndexOf("/")+1);
        currentTime = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        try {
            //删除本地目录过期文件，删除空文件夹
            deleteFilesFromLocal(new File(localDirectory));
            //删除远程目录过期文件，删除空文件夹
            deletesFromRemote(remoteDirectory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteFilesFromLocal(File file){
        if(flag){
            if (file.isDirectory()) {
                //是文件夹，遍历文件夹下所有文件或文件夹
                File[] list = file.listFiles();
                if(list.length>0){
                    for (File f : list) {
                        deleteFilesFromLocal(f);
                    }
                }
            } else {
                if(currentTime-file.lastModified()>expiredTime&&flag==true){
                    file.delete();
                    fileDeletedCount++;
                    if(!switchBotton&&fileDeletedCount==fileQunatity){
                        flag = false;
                    }
                }
            }
        }
        deleteLocalEmptyDir(new File(localDirectory));
    }

    /**
     * 清除空文件夹
     * @param file
     */
    private void deleteLocalEmptyDir(File file){
        if(file.isDirectory()){
            File[] list = file.listFiles();
            if(list.length==0){
                if(!localRootFilePath.equals(file.getName())){
                    file.delete();
                }
            }else{
                File[] lists = file.listFiles();
                for (File f : lists) {
                    deleteLocalEmptyDir(f);
                }
            }
        }
    }

    private void deletesFromRemote(String directory) throws SftpException {
        ChannelSftp sftp = SftpUtils.connect(remoteIp, remotePort, remoteUser, remotePassword);
        //删除过期文件
        SftpUtils.deleteExpiredFile(sftp,null,directory,expiredTime,switchBotton,fileQunatity);
        sftp.disconnect();
    }

}
