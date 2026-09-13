package com.example.chitchat.service;

import com.example.chitchat.dto.AuthResponse;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

@Service
public class JWTService {

    private String secretSigningKey;
    public JWTService(@Value("${spring.auth.jwt-secret}") String signingKey){
        this.secretSigningKey = signingKey;
    }

    public String generateAccessToken(String username) throws JOSEException{
        Date now = new Date();
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder().subject(username).
                                                            issuer("om anand").
                                                            audience("chitchatpeople").
                                                            issueTime(now).
                                                            expirationTime(new Date(now.getTime() + 8 * 60 * 60 * 1000)).
                                                            claim("type","access").
                                                            build();
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        SignedJWT signedJwt = new SignedJWT(header, claimSet);
        JWSSigner signer = new MACSigner(secretSigningKey);
        signedJwt.sign(signer);
        return signedJwt.serialize();
    }


    public String generateRefreshToken(String username) throws JOSEException{
        Date now = new Date();
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder().subject(username).
                issuer("om anand").
                audience("chitchatpeople").
                issueTime(now).
                expirationTime(new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000)).
                claim("type","refresh").
                build();
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        SignedJWT signedJwt = new SignedJWT(header, claimSet);
        JWSSigner signer = new MACSigner(secretSigningKey);
        signedJwt.sign(signer);
        return signedJwt.serialize();
    }

    public boolean validateAccessToken( String accessToken ) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(accessToken);
        JWSVerifier verifier = new MACVerifier(secretSigningKey);

        boolean valid = signedJWT.verify(verifier);
        if( !valid ) return false;

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        Date expiration = claims.getExpirationTime();
        String tokenType = claims.getClaimAsString("type");

        if( !"access".equals(tokenType) || expiration==null || expiration.before(new Date()) ) return false;
        return true;
    }
    public boolean validateRefreshToken( String refreshToken ) throws ParseException,JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(refreshToken);
        JWSVerifier verifier = new MACVerifier(secretSigningKey);

        boolean valid = signedJWT.verify(verifier);
        if( !valid ) return false;

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        Date expiration = claims.getExpirationTime();
        String tokenType = claims.getClaimAsString("type");

        if( !"refresh".equals(tokenType) || expiration==null || expiration.before(new Date()) ) return false;
        return true;
    }

    public String getUsernameFromToken( String accessToken ) throws ParseException{
        SignedJWT signedJWT = SignedJWT.parse(accessToken);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        return claims.getSubject();
    }

}
