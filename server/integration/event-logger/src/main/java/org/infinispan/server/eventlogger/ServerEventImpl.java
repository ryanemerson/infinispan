package org.infinispan.server.eventlogger;

import java.time.Instant;
import java.util.Optional;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.util.logging.events.EventLog;
import org.infinispan.util.logging.events.EventLogCategory;
import org.infinispan.util.logging.events.EventLogLevel;

/**
 * ServerEvent.
 *
 * @author Tristan Tarrant
 * @since 8.2
 */
public class ServerEventImpl implements EventLog {

   @ProtoField(number = 1, required = true)
   EventLogLevel level;

   @ProtoField(number = 2, required = true)
   EventLogCategory category;

   @ProtoField(number = 3, required = true)
   String message;

   Instant when;
   Optional<String> detail;
   Optional<String> context;
   Optional<String> who;
   Optional<String> scope;

   ServerEventImpl() {}

   ServerEventImpl(EventLogLevel level, EventLogCategory category, Instant when, String message, Optional<String> detail, Optional<String> context, Optional<String> who, Optional<String> scope) {
      this.level = level;
      this.category = category;
      this.message = message;
      this.when = when;
      this.detail = detail;
      this.context = context;
      this.who = who;
      this.scope = scope;
   }

   ServerEventImpl(EventLogLevel level, EventLogCategory category, Instant when, String message) {
      this(level, category, when, message, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
   }

   @Override
   public Instant getWhen() {
      return when;
   }

   @Override
   public EventLogLevel getLevel() {
      return level;
   }

   @Override
   public EventLogCategory getCategory() {
      return category;
   }

   @Override
   public String getMessage() {
      return message;
   }

   @Override
   public Optional<String> getDetail() {
      return detail;
   }

   @Override
   public Optional<String> getWho() {
      return who;
   }

   @Override
   public Optional<String> getContext() {
      return context;
   }

   @Override
   public Optional<String> getScope() {
      return scope;
   }

   @ProtoField(number = 4, name = "epoch", required = true)
   long getEpoch() {
      return when.getEpochSecond();
   }

   @ProtoField(number = 6, name = "detail")
   String getNullableDetail() {
      return detail.orElse(null);
   }

   @ProtoField(number = 7, name = "who")
   String getNullableWho() {
      return who.orElse(null);
   }

   @ProtoField(number = 8, name = "context")
   String getNullableContext() {
      return context.orElse(null);
   }

   @ProtoField(number = 9, name = "scope")
   String getNullableScope() {
      return scope.orElse(null);
   }

   void setEpoch(long epoch) {
      this.when = Instant.ofEpochSecond(epoch);
   }

   void setNullableDetail(String detail) {
      this.detail = Optional.ofNullable(detail);
   }

   void setNullableContext(String context) {
      this.context = Optional.ofNullable(context);
   }

   void setNullableWho(String who) {
      this.who = Optional.ofNullable(who);
   }

   void setNullableScope(String scope) {
      this.scope = Optional.ofNullable(scope);
   }

   @Override
   public int compareTo(EventLog that) {
      // Intentionally backwards
      return that.getWhen().compareTo(this.when);
   }

   @Override
   public String toString() {
      return "ServerEventImpl{" +
            "level=" + level +
            ", category=" + category +
            ", when=" + when +
            ", message='" + message + '\'' +
            ", detail=" + detail +
            ", context=" + context +
            ", who=" + who +
            ", scope=" + scope +
            '}';
   }
}
