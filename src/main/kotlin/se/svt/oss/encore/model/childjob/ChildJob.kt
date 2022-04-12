package se.svt.oss.encore.model.childjob

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import java.util.UUID

// @JsonTypeInfo(use = Jso nTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@RedisHash("encore-child-jobs", timeToLive = (60 * 60 * 24 * 7).toLong()) // 1 week ttl
data class ChildJob(
    @Id
    val id: UUID = UUID.randomUUID(),
    val parentId: UUID,
    val name: String,
    val commands: String,
    val idx: Int,
    val status: String = "NEW",
    val key: String = "encore-child-jobs:${id}",
    val parentKey: String = "encore-jobs:${parentId}"
)

