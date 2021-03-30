package com.uaepay.basis.compensation.event.dal;

import java.util.List;

import org.apache.ibatis.annotations.*;

import com.uaepay.basis.compensation.event.dal.domain.CompensationEventDO;
import com.uaepay.basis.compensation.event.dal.extend.SimpleSelectExtendedLanguageDriver;

/**
 * @author cc
 */
public interface CompensationEventMapper {

    /**
     * 保存事件
     * 
     * @param event
     *            事件
     * @param allowDelaySeconds
     *            允许时间延迟
     */
    @Insert("insert into t_compensation_event (event_key, main_type, sub_type, execute_status, execute_count, extension, allow_time, create_time, update_time)"
        + " values (#{event.eventKey}, #{event.mainType}, #{event.subType}, 'I', 0, #{event.extension}, now() + interval #{allowDelaySeconds} second, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "event.eventId", keyColumn = "event_id")
    void insert(@Param("event") CompensationEventDO event, @Param("allowDelaySeconds") long allowDelaySeconds);

    /**
     * 根据eventId获取满足执行时间的事件
     * 
     * @param eventId
     *            事件id
     * @return 查询结果，可能为null
     */
    @Select("select event_id, event_key, main_type, sub_type, execute_status, execute_count, extension "
        + "  from t_compensation_event where event_id = #{eventId} and execute_status in ('I', 'E') and allow_time <= now()")
    @Results(id = "eventMap", value = {@Result(column = "event_id", property = "eventId"),
        @Result(column = "event_key", property = "eventKey"), @Result(column = "main_type", property = "mainType"),
        @Result(column = "sub_type", property = "subType"),
        @Result(column = "execute_status", property = "executeStatus"),
        @Result(column = "execute_count", property = "executeCount"),
        @Result(column = "extension", property = "extension"), @Result(column = "allow_time", property = "allowTime")})
    CompensationEventDO getAllowByEventId(@Param("eventId") Long eventId);

    /**
     * 根据业务唯一约束获取id
     * 
     * @param eventKey
     *            事件key
     * @param mainType
     *            主类型
     * @param subType
     *            子类型
     * @return 事件id
     */
    @Select("select event_id from t_compensation_event where event_key = #{eventKey} and main_type = #{mainType} and sub_type = #{subType}")
    Long getEventIdByBizKey(@Param("eventKey") String eventKey, @Param("mainType") String mainType,
        @Param("subType") String subType);

    /**
     * 获取满足执行时间的事件
     * 
     * @param batchSize
     *            批次大小
     * @return 查询结果
     */
    @Select("select event_id, event_key, main_type, sub_type, execute_status, execute_count, extension"
        + "  from t_compensation_event where execute_status in ('I', 'E') and allow_time <= now() and event_id % #{shardingCount} = #{shardingIndex}"
        + "  order by allow_time limit #{batchSize}")
    @ResultMap("eventMap")
    List<CompensationEventDO> listRetryEvent(@Param("batchSize") int batchSize,
        @Param("shardingIndex") int shardingIndex, @Param("shardingCount") int shardingCount);

    /**
     * 获取满足执行时间的事件
     *
     * @param batchSize
     *            批次大小
     * @return 查询结果
     */
    @Lang(SimpleSelectExtendedLanguageDriver.class)
    @Select("select event_id, event_key, main_type, sub_type, execute_status, execute_count, extension"
        + "  from t_compensation_event where execute_status in ('I', 'E') and allow_time &lt;= now() "
        + "    and event_id % #{shardingCount} = #{shardingIndex} and main_type in (#{handlerCodeWhiteList})"
        + "  order by allow_time limit #{batchSize}")
    @ResultMap("eventMap")
    List<CompensationEventDO> listRetryEventWithMainTypeList(@Param("batchSize") int batchSize,
        @Param("shardingIndex") int shardingIndex, @Param("shardingCount") int shardingCount,
        @Param("handlerCodeWhiteList") List<String> handlerCodeWhiteList);

    /**
     * 删除事件
     * 
     * @param eventId
     *            事件id
     * @return 更新行数
     */
    @Delete("delete from t_compensation_event where event_id = #{eventId}")
    int deleteByEventId(@Param("eventId") Long eventId);

    /**
     * 更新异常
     * 
     * @param eventId
     *            事件id
     * @param errorMessage
     *            错误信息
     * @param allowTimeDelaySeconds
     *            允许时间延迟秒数
     * @param preStatus
     *            前状态
     * @return 影响行数
     */
    @Update("update t_compensation_event set execute_status = 'E', execute_count = execute_count + 1, error_message = #{errorMessage}"
        + "    , allow_time = now() + interval #{allowTimeDelaySeconds} second, update_time = now()"
        + "  where event_id = #{eventId} and execute_status = #{preStatus}")
    int updateError(@Param("eventId") Long eventId, @Param("errorMessage") String errorMessage,
        @Param("allowTimeDelaySeconds") long allowTimeDelaySeconds, @Param("preStatus") String preStatus);

    /**
     * 更新失败
     * 
     * @param eventId
     *            事件id
     * @param errorMessage
     *            错误信息
     * @param preStatus
     *            前状态
     * 
     * @return 影响行数
     */
    @Update("update t_compensation_event set execute_status = 'F', execute_count = execute_count + 1, error_message = #{errorMessage}"
        + "    , update_time = now() where event_id = #{eventId} and execute_status = #{preStatus}")
    int updateFail(@Param("eventId") Long eventId, @Param("errorMessage") String errorMessage,
        @Param("preStatus") String preStatus);

}
