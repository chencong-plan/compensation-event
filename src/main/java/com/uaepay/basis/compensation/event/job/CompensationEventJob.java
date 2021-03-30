package com.uaepay.basis.compensation.event.job;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.uaepay.basis.compensation.event.domainservice.CompensationEventRetryService;
import com.uaepay.job.starter.autoconfigure.annotation.ElasticJobConfig;
import com.uaepay.job.starter.base.AbstractSimpleJob;

/**
 * 事件补偿重试
 * 
 * @author cc
 */
@ElasticJobConfig(jobCode = "${spring.application.name}_compensationEventJob${compensation.event.job.suffix:}",
    cron = "${compensation.event.job.crontab}", shardingCount = "${compensation.event.job.sharding-count}",
    description = "事件补偿定时任务", disabledEx = "${compensation.event.job.disabled:false}",
    overwriteEx = "${compensation.event.job.overwrite:false}")
public class CompensationEventJob extends AbstractSimpleJob {

    @Autowired
    CompensationEventRetryService compensationEventRetryService;

    @Value("${compensation.event.job.handlerCodeWhiteList:}")
    List<String> handlerCodeWhiteList;

    @Override
    protected String jobName() {
        return "CompensationEventJob";
    }

    @Override
    protected void processJob(ShardingContext shardingContext, String jobTraceId) {
        compensationEventRetryService.retry(shardingContext.getShardingItem(), shardingContext.getShardingTotalCount(),
            handlerCodeWhiteList);
    }

}
