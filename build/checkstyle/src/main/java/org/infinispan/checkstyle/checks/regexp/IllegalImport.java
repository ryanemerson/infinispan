package org.infinispan.checkstyle.checks.regexp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;

/**
 * A simple CheckStyle checker to verify specific import statements are not being used.
 *
 * @author Sanne Grinovero
 */
public class IllegalImport extends com.puppycrawl.tools.checkstyle.checks.imports.IllegalImportCheck {

   private final List<Pattern> skipPkgsRegexps = new ArrayList<>();
   private String[] skipPkgs;

   public String[] getSkipPkgs() {
      return skipPkgs;
   }

   public void setSkipPkgs(String... from) {
      skipPkgs = from.clone();
      for (String illegalClass : skipPkgs) {
         skipPkgsRegexps.add(CommonUtil.createPattern(illegalClass));
      }
   }

   @Override
   public int[] getRequiredTokens() {
      return new int[] {TokenTypes.PACKAGE_DEF, TokenTypes.IMPORT, TokenTypes.STATIC_IMPORT};
   }

   @Override
   public void visitToken(DetailAST ast) {
      if (ast.getType() == TokenTypes.PACKAGE_DEF) {

      }
   }
}
