package com.example.housekeepingservice.config;

import com.example.housekeepingservice.scheduledtasks.CronDeleteExpiredFile;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfiguration {

    @Bean
    public JobDetail jobDetail(){
        return JobBuilder.newJob(CronDeleteExpiredFile.class).storeDurably().build();
    }

    @Bean
    public Trigger trigger(){
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(1) //每一小时执行一次定时任务
                .repeatForever(); //永久重复执行
        return TriggerBuilder.newTrigger().forJob(jobDetail()).withSchedule(scheduleBuilder).build();
    }
}
