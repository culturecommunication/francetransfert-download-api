package fr.gouv.culture.francetransfert.application.services;

import com.kosprov.jargon2.api.Jargon2.*;
import org.springframework.stereotype.Service;

import static com.kosprov.jargon2.api.Jargon2.jargon2Hasher;
import static com.kosprov.jargon2.api.Jargon2.jargon2Verifier;

@Service
public class PasswordHasherServices {
    private final static Type TYPE = Type.ARGON2d;
    private final static int MEMORY_COST = 65536;
    private final static int TIME_COST = 3;
    private final static int PARALLELISM = 4;
    private final static int HASH_LENGTH = 16;
    private final static byte[] FRANCE_TRANSFERT_SALT = "France Transfert salt".getBytes();

    /**
     *
     * @return Configure the hasher
     */
    public Hasher configureHasher() {
        Hasher hasher = jargon2Hasher()
                .type(TYPE)
                .memoryCost(MEMORY_COST)
                .timeCost(TIME_COST)
                .parallelism(PARALLELISM)
                .hashLength(HASH_LENGTH);
        return hasher;
    }

    /**
     *
     * @return Configure the verifier with the same settings as the hasher
     */
    public Verifier configureVerifyHasher() {
        Verifier verify = jargon2Verifier()
                .type(TYPE)
                .memoryCost(MEMORY_COST)
                .timeCost(TIME_COST)
                .parallelism(PARALLELISM);
        return verify;
    }

    /**
     *
     * @param password
     * @return Set the salt and password to calculate the raw hash
     */
    public String calculatePasswordHashed(String password) {
        Hasher hasher = configureHasher();
        byte[] rawHash = hasher.salt(FRANCE_TRANSFERT_SALT).password(password.getBytes()).rawHash();
        return new String(rawHash);
    }

    /**
     *
     * @param password
     * @return Set the raw hash, salt and password and verify
     */
    public boolean verifyPasswordHashed(String password) {
        byte[] passwordInByte = password.getBytes();
        Hasher hasher = configureHasher();
        byte[] rawHash = hasher.salt(FRANCE_TRANSFERT_SALT).password(passwordInByte).rawHash();

        Verifier verify = configureVerifyHasher();
        boolean matches = verify.hash(rawHash).salt(FRANCE_TRANSFERT_SALT).password(passwordInByte).verifyRaw();
        return matches;
    }
}
