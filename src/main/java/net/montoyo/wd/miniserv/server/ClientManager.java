/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.server;

import net.montoyo.wd.miniserv.KeyParameters;
import net.montoyo.wd.utilities.Log;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientManager {

    private final SecureRandom random = new SecureRandom(); //SecureRandom is thread safe
    private final HashMap<UUID, byte[]> keys = new HashMap<>();
    private final ReentrantReadWriteLock keyLock = new ReentrantReadWriteLock();

    public byte[] getOrGenClientKey(UUID uuid) {
        keyLock.readLock().lock();
        byte[] key = keys.get(uuid);
        keyLock.readLock().unlock();

        if(key == null) {
            key = new byte[KeyParameters.KEY_SIZE];
            random.nextBytes(key);

            keyLock.writeLock().lock();
            keys.put(uuid, key);
            keyLock.writeLock().unlock();
        }

        return key;
    }

    public byte[] getClientKey(UUID uuid) {
        keyLock.readLock().lock();
        byte[] ret = keys.get(uuid);
        keyLock.readLock().unlock();

        return ret;
    }

    public void revokeClientKey(UUID id) {
        keyLock.writeLock().lock();
        keys.remove(id);
        keyLock.writeLock().unlock();
    }

    public byte[] generateChallenge() {
        byte[] ret = new byte[KeyParameters.CHALLENGE_SIZE];
        random.nextBytes(ret);
        return ret;
    }

    public boolean verifyClient(UUID client, byte[] challenge, byte[] hmac) {
        keyLock.readLock().lock();
        byte[] key = keys.get(client);
        keyLock.readLock().unlock();

        if(challenge == null || hmac == null || key == null)
            return false;

        try {
            Mac mac = Mac.getInstance(KeyParameters.MAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, KeyParameters.MAC_ALGORITHM));
            byte[] result = mac.doFinal(challenge);

            return Arrays.equals(hmac, result);
        } catch(NoSuchAlgorithmException ex) {
            Log.warningEx("%s is not supported?!?!", ex, KeyParameters.MAC_ALGORITHM);
        } catch(InvalidKeyException ex) {
            Log.warningEx("The generated key is invalid", ex);
        }

        return false;
    }

    public byte[] encryptClientKey(UUID client, byte[] modulus, byte[] exponent) {
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(exponent));

        try {
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(keySpec);
            Cipher cipher = Cipher.getInstance(KeyParameters.RSA_CIPHER);

            cipher.init(Cipher.ENCRYPT_MODE, key, random);
            return cipher.doFinal(getOrGenClientKey(client));
        } catch(NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Log.warningEx("%s is not supported?!?!", ex, KeyParameters.RSA_CIPHER);
        } catch(InvalidKeySpecException | InvalidKeyException ex) {
            Log.warningEx("A client sent a malicious key", ex);
        } catch(IllegalBlockSizeException | BadPaddingException ex) {
            Log.warningEx("Could not encrypt client key", ex);
        }

        return null;
    }

}
