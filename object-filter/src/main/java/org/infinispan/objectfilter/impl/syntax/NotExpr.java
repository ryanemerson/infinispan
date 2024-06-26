package org.infinispan.objectfilter.impl.syntax;

/**
 * @author anistor@redhat.com
 * @since 7.0
 */
public final class NotExpr implements BooleanExpr {

   private final BooleanExpr child;

   public NotExpr(BooleanExpr child) {
      this.child = child;
   }

   public BooleanExpr getChild() {
      return child;
   }

   @Override
   public <T> T acceptVisitor(Visitor<?, ?> visitor) {
      return (T) visitor.visit(this);
   }

   @Override
   public String toString() {
      return "NOT(" + child + ')';
   }

   @Override
   public void appendQueryString(StringBuilder sb) {
      sb.append("NOT(");
      child.appendQueryString(sb);
      sb.append(")");
   }
}
