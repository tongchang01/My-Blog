package com.tyb.myblog.v2.system.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tyb.myblog.v2.common.infrastructure.persistence.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 附件持久化实体，对应 {@code t_attachment}。
 */
@Getter
@Setter
@TableName("t_attachment")
public class AttachmentEntity extends BaseEntity {

    /** 物理存储类型。 */
    private String storageType;

    /** 存储桶或本地根目录别名。 */
    private String bucket;

    /** 存储对象键。 */
    private String objectKey;

    /** 对外访问地址。 */
    private String publicUrl;

    /** 服务端识别的 MIME。 */
    private String contentType;

    /** 文件字节数。 */
    private Long fileSize;

    /** 图片宽度。 */
    private Integer width;

    /** 图片高度。 */
    private Integer height;

    /** 清洗后的原始文件名。 */
    private String originalFilename;

    /** 小写 SHA-256。 */
    private String hashSha256;
}
