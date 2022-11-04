package com.exadel.frs.system.security;

import java.sql.Types;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

public class CustomJdbcTokenStore extends JdbcTokenStore {

    private static final String INSERT_ACCESS_TOKEN_WITH_EXPIRATION_SQL = "insert into oauth_access_token (token_id, token, authentication_id, user_name, client_id, authentication, refresh_token, expiration) values (?, ?, ?, ?, ?, ?, ?,?)";
    private static final String INSERT_REFRESH_TOKEN_WITH_EXPIRATION_SQL = "insert into oauth_refresh_token (token_id, token, authentication, expiration) values (?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public CustomJdbcTokenStore(DataSource dataSource) {
        super(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        String refreshToken = getRefreshToken(token);
        if (this.readAccessToken(token.getValue()) != null) {
            this.removeAccessToken(token.getValue());
        }

        DefaultAuthenticationKeyGenerator defaultAuthenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();

        this.jdbcTemplate.update(
                INSERT_ACCESS_TOKEN_WITH_EXPIRATION_SQL,
                new Object[]{this.extractTokenKey(token.getValue()),
                        new SqlLobValue(this.serializeAccessToken(token)),
                        defaultAuthenticationKeyGenerator.extractKey(authentication),
                        authentication.isClientOnly() ? null : authentication.getName(),
                        authentication.getOAuth2Request().getClientId(),
                        new SqlLobValue(this.serializeAuthentication(authentication)),
                        this.extractTokenKey(refreshToken), token.getExpiration()},
                new int[]{Types.VARCHAR, Types.BLOB, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BLOB, Types.VARCHAR, Types.TIMESTAMP}
        );
    }

    private String getRefreshToken(OAuth2AccessToken token) {
        if (token.getRefreshToken() != null) {
            return token.getRefreshToken().getValue();
        }
        return null;
    }

    @Override
    public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
        DefaultExpiringOAuth2RefreshToken oAuth2RefreshToken = (DefaultExpiringOAuth2RefreshToken) refreshToken;
        this.jdbcTemplate.update(
                INSERT_REFRESH_TOKEN_WITH_EXPIRATION_SQL,
                new Object[]{this.extractTokenKey(refreshToken.getValue()), new SqlLobValue(this.serializeRefreshToken(refreshToken)), new SqlLobValue(
                        this.serializeAuthentication(authentication)), oAuth2RefreshToken.getExpiration()},
                new int[]{Types.VARCHAR, Types.BLOB, Types.BLOB, Types.TIMESTAMP}
        );
    }
}
