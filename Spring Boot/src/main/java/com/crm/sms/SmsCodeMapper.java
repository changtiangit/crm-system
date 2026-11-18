package com.crm.sms;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SmsCodeMapper {

    @Insert("INSERT INTO sms_codes(phone, scene, code_hash, expire_at) " +
            "VALUES(#{phone}, #{scene}, #{codeHash}, #{expireAt})")
    int insert(@Param("phone") String phone,
               @Param("scene") String scene,
               @Param("codeHash") String codeHash,
               @Param("expireAt") LocalDateTime expireAt);

    @Select("SELECT id, phone, scene, code_hash, attempts, used, expire_at, created_at FROM sms_codes " +
            "WHERE phone = #{phone} AND scene = #{scene} ORDER BY id DESC LIMIT 1")
    SmsCodeRow findLatest(@Param("phone") String phone, @Param("scene") String scene);

    @Select("SELECT id, phone, scene, code_hash, attempts, used, expire_at, created_at FROM sms_codes " +
            "WHERE phone = #{phone} AND scene = #{scene} AND used = 0 AND expire_at > NOW() " +
            "ORDER BY id DESC LIMIT 1")
    SmsCodeRow findLatestValid(@Param("phone") String phone, @Param("scene") String scene);

    @Select("SELECT COUNT(*) FROM sms_codes " +
            "WHERE phone = #{phone} AND scene = #{scene} AND created_at >= #{since}")
    int countSince(@Param("phone") String phone,
                   @Param("scene") String scene,
                   @Param("since") LocalDateTime since);

    @Update("UPDATE sms_codes SET attempts = attempts + 1 WHERE id = #{id}")
    int increaseAttempts(@Param("id") Long id);

    @Update("UPDATE sms_codes SET used = 1 WHERE id = #{id}")
    int markUsed(@Param("id") Long id);

    /** 验证通过后把该手机号此前未用的验证码一并作废 */
    @Update("UPDATE sms_codes SET used = 1 WHERE phone = #{phone} AND scene = #{scene} AND used = 0")
    int invalidateAll(@Param("phone") String phone, @Param("scene") String scene);
}
