package com.uaepay.basis.compensation.event.domainservice.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.skywalking.apm.toolkit.trace.CallableWrapper;
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.uaepay.basis.compensation.event.dal.CompensationEventMapper;
import com.uaepay.basis.compensation.event.domain.BatchExecuteResult;
import com.uaepay.basis.compensation.event.domain.CompensationEvent;
import com.uaepay.basis.compensation.event.domain.CompensationProperties;
import com.uaepay.basis.compensation.event.domain.ExecuteResult;
import com.uaepay.basis.compensation.event.domain.convertor.impl.CompensationEventConverter;
import com.uaepay.basis.compensation.event.domainservice.CompensationEventExecutor;
import com.uaepay.basis.compensation.event.service.CompensationEventHandler;

/**
 * 事件执行器
 * 
 * @author zc
 */
@Service
public class CompensationEventExecutorImpl implements CompensationEventExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompensationEventExecutorImpl.class);

    public static final int ERROR_MESSAGE_MAX_WIDTH = 100;
    private static final String THREAD_NAME_PREFIX = "COMPENSATION_EVENT";

    @Autowired
    private CompensationProperties compensationProperties;

    @Autowired
    private CompensationEventHandlerFactory compensationEventHandlerFactory;

    @Autowired
    private CompensationEventMapper compensationEventMapper;

    @Autowired
    private CompensationEventConverter compensationEventConverter;

    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @PostConstruct
    public void init() {
        threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        CompensationProperties.PoolConfig poolConfig = compensationProperties.getPoolConfig();
        threadPoolTaskExecutor.setCorePoolSize(poolConfig.getCorePoolSize());
        threadPoolTaskExecutor.setMaxPoolSize(poolConfig.getMaxPoolSize());
        threadPoolTaskExecutor.setQueueCapacity(poolConfig.getQueueCapacity());
        threadPoolTaskExecutor.setAllowCoreThreadTimeOut(true);
        threadPoolTaskExecutor.setKeepAliveSeconds(poolConfig.getKeepAliveSeconds());
        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        threadPoolTaskExecutor.initialize();
    }

    @Override
    public void asyncExecute(CompensationEvent event) {
        threadPoolTaskExecutor.submit(RunnableWrapper.of(() -> execute(event)));
    }

    @Override
    public BatchExecuteResult batchExecute(List<CompensationEvent> events) {
        List<Future<ExecuteResult>> futures = new ArrayList<>(events.size());
        for (CompensationEvent event : events) {
            Future<ExecuteResult> future = threadPoolTaskExecutor.submit(new CallableWrapper<>(() -> execute(event)));
            futures.add(future);
        }
        BatchExecuteResult result = new BatchExecuteResult();
        for (Future<ExecuteResult> future : futures) {
            try {
                ExecuteResult itemResult = future.get();
                if (itemResult == null) {
                    result.increaseSkipCount();
                } else {
                    result.increaseCount(itemResult.getStatus());
                }
            } catch (Throwable e) {
                result.increaseErrorCount();
            }
        }
        return result;
    }

    /**
     * 执行事件
     * 
     * @param event
     *            执行事件
     * @return 如果实际未执行，则返回null；否则返回执行结果，非空
     */
    @Override
    public ExecuteResult execute(CompensationEvent event) {
        LOGGER.info("执行补偿事件: id={}, key={}, mainType={}, subType={}", event.getEventId(), event.getEventKey(),
            event.getMainType(), event.getSubType());
        ExecuteResult result = null;
        try {
            // 校验原始参数，如果不通过，不更新结果
            if (event.getEventId() == null || event.getExecuteStatus() == null) {
                result = ExecuteResult.error("事件字段有误:" + event);
                return result;
            }
            // 查询数据库，如果不存在可执行的，则不更新结果
            if (!event.isSkipCheckBeforeExecute()) {
                CompensationEvent existEvent = compensationEventConverter
                    .convertToBO(compensationEventMapper.getAllowByEventId(event.getEventId()));
                if (existEvent == null) {
                    return null;
                } else {
                    event = existEvent;
                }
            }
            // 处理器处理
            CompensationEventHandler handler = compensationEventHandlerFactory.getService(event.getMainType());
            if (handler == null) {
                result = ExecuteResult.fail("没有匹配的补偿事件处理器");
            } else {
                result = handler.handleEvent(event);
            }
        } catch (Throwable e) {
            LOGGER.error("执行事件异常", e);
            result = ExecuteResult.error(e.getMessage());
        } finally {
            LOGGER.info("执行事件结果: {}", result);
        }

        // 更新执行结果
        return updateExecuteResult(event, result);
    }

    private ExecuteResult updateExecuteResult(CompensationEvent event, ExecuteResult result) {
        if (result == null) {
            return null;
        }
        try {
            switch (result.getStatus()) {
                case SUCCESS:
                    updateExecuteSuccess(event);
                    break;
                case FAIL:
                    updateExecuteFail(event, result.getErrorMessage());
                    break;
                case ERROR:
                    updateExecuteError(event, result.getErrorMessage());
                    break;
                default:
                    result = ExecuteResult.error("执行结果状态错误: " + result.getStatus());
                    updateExecuteError(event, result.getErrorMessage());
                    break;
            }
            return result;
        } catch (Throwable e) {
            LOGGER.error("更新执行结果异常", e);
            return ExecuteResult.error(e.getMessage());
        }
    }

    private void updateExecuteSuccess(CompensationEvent event) {
        int count = compensationEventMapper.deleteByEventId(event.getEventId());
        if (count != 1) {
            LOGGER.warn("删除补偿事件异常");
        }
    }

    private void updateExecuteFail(CompensationEvent event, String errorMessage) {
        errorMessage = StringUtils.abbreviate(errorMessage, ERROR_MESSAGE_MAX_WIDTH);
        int count =
            compensationEventMapper.updateFail(event.getEventId(), errorMessage, event.getExecuteStatus().getCode());
        if (count != 1) {
            LOGGER.error("更新补偿事件为失败异常");
        } else {
            LOGGER.info("更新补偿事件为失败");
        }
    }

    private void updateExecuteError(CompensationEvent event, String errorMessage) {
        if (compensationProperties.getRetryIntervals() == null
            || event.getExecuteCount() >= compensationProperties.getRetryIntervals().size()) {
            // 超过次数，更新为失败
            updateExecuteFail(event, errorMessage);
        } else {
            errorMessage = StringUtils.abbreviate(errorMessage, ERROR_MESSAGE_MAX_WIDTH);
            // 延迟通知
            Duration allowDelayInterval = compensationProperties.getRetryIntervals().get(event.getExecuteCount());
            int count = compensationEventMapper.updateError(event.getEventId(), errorMessage,
                allowDelayInterval.getSeconds(), event.getExecuteStatus().getCode());
            if (count != 1) {
                LOGGER.warn("更新补偿事件为异常异常");
            }
        }
    }

}
