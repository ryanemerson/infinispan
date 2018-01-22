package org.infinispan.server.jgroups.security;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

import org.infinispan.server.jgroups.logging.JGroupsLogger;
import org.wildfly.security.auth.callback.CredentialCallback;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.interfaces.DigestPassword;
import org.wildfly.security.password.spec.DigestPasswordSpec;
import org.wildfly.security.sasl.util.UsernamePasswordHashUtil;

/**
 * SaslClientCallbackHandler.
 *
 * @author Tristan Tarrant
 */
public class SaslClientCallbackHandler implements CallbackHandler {
    private final String realm;
    private final String name;
    private final String credential;

    public SaslClientCallbackHandler(String realm, String name, String credential) {
        this.realm = realm;
        this.name = name;
        this.credential = credential;
    }

    public SaslClientCallbackHandler(String name, String credential) {
        int realmSep = name.indexOf('@');
        this.realm = realmSep < 0 ? "" : name.substring(realmSep+1);
        this.name = realmSep < 0 ? name : name.substring(0, realmSep);
        this.credential = credential;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            JGroupsLogger.ROOT_LOGGER.errorf("SaslClientCallbackHandler CALLBACK: %s" + callback.getClass().getName());
            if (callback instanceof PasswordCallback) {
                ((PasswordCallback) callback).setPassword(credential.toCharArray());
            } else if (callback instanceof NameCallback) {
                try {
                    // TODO remove I don't think this is necessary
                    // Potential hack for ELY-1426 and ELY-991
                    Field defaultName = callback.getClass().getDeclaredField("defaultName");
                    defaultName.setAccessible(true);
                    defaultName.set(callback, name);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    JGroupsLogger.ROOT_LOGGER.debug(e);
                }
                ((NameCallback) callback).setName(name);
            } else if (callback instanceof RealmCallback) {
               ((RealmCallback) callback).setText(realm);
            } else if (callback instanceof CredentialCallback) {
                CredentialCallback cb = (CredentialCallback) callback;
                Password password;
                try {
                    byte[] urpHash = new UsernamePasswordHashUtil().generateHashedURP(name, realm, credential.toCharArray());
                    KeySpec keySpec = new DigestPasswordSpec(name, realm, urpHash);
                    PasswordFactory passwordFactory = PasswordFactory.getInstance(DigestPassword.ALGORITHM_DIGEST_MD5);
                    password = passwordFactory.generatePassword(keySpec);

                    JGroupsLogger.ROOT_LOGGER.errorf("Raw Credential : %s", credential);
                    JGroupsLogger.ROOT_LOGGER.errorf("credentialBytes: %s", Arrays.toString(credential.getBytes()));
                    JGroupsLogger.ROOT_LOGGER.errorf("urpHash        : %s", Arrays.toString(urpHash));
                    JGroupsLogger.ROOT_LOGGER.errorf("passwordDigest : %s", Arrays.toString(((DigestPassword) password).getDigest()));

////                    Password password = PasswordFactory.getInstance(cb.getAlgorithm());
////                    ((CredentialCallback) callback).setCredential(new PasswordCredential(password));
//                    JGroupsLogger.ROOT_LOGGER.errorf("%s: Algorithm: %s", callback, ((CredentialCallback) callback).getAlgorithm());
//                    JGroupsLogger.ROOT_LOGGER.errorf("%s: Algorithm: %s", callback, ((CredentialCallback) callback).getAlgorithm());
//                    JGroupsLogger.ROOT_LOGGER.errorf("%s: Credential: %s", callback, ((CredentialCallback) callback).getCredential());
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new IllegalArgumentException(e);
                }
//                password = DigestPassword.createRaw(DigestPassword.ALGORITHM_DIGEST_MD5, name, realm, credential.getBytes());
                cb.setCredential(new PasswordCredential(password));
            }
        }
    }

}
