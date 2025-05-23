package org.infinispan.server.resp.commands.connection;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.commands.Resp3Command;
import org.infinispan.server.resp.serialization.ResponseWriter;

import io.netty.channel.ChannelHandlerContext;

/**
 * DBSIZE
 *
 * @see <a href="https://redis.io/commands/dbsize/">DBSIZE</a>
 * @since 15.0
 */
public class DBSIZE extends RespCommand implements Resp3Command {
   public DBSIZE() {
      super(1, 0, 0, 0, AclCategory.KEYSPACE.mask() | AclCategory.READ.mask() | AclCategory.FAST.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {
      return handler.stageToReturn(handler.cache().sizeAsync(), ctx, ResponseWriter.INTEGER);
   }
}
