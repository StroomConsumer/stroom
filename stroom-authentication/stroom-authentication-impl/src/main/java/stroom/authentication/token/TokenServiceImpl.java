package stroom.authentication.token;

import org.jose4j.jwk.JsonWebKey;
import stroom.authentication.account.Account;
import stroom.authentication.account.AccountDao;
import stroom.authentication.account.AccountService;
import stroom.authentication.config.TokenConfig;
import stroom.authentication.exceptions.NoSuchUserException;
import stroom.security.api.SecurityContext;
import stroom.security.shared.PermissionException;
import stroom.security.shared.PermissionNames;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.time.Instant;
import java.util.Optional;

public class TokenServiceImpl implements TokenService {
    private final JwkCache jwkCache;
    private final TokenDao tokenDao;
    private final AccountDao accountDao;
    private final SecurityContext securityContext;
    private final AccountService accountService;
    private final TokenBuilderFactory tokenBuilderFactory;
    private final TokenConfig tokenConfig;

    @Inject
    TokenServiceImpl(final JwkCache jwkCache,
                     final TokenDao tokenDao,
                     final AccountDao accountDao,
                     final SecurityContext securityContext,
                     final AccountService accountService,
                     final TokenBuilderFactory tokenBuilderFactory,
                     final TokenConfig tokenConfig) {
        this.jwkCache = jwkCache;
        this.tokenDao = tokenDao;
        this.accountDao = accountDao;
        this.securityContext = securityContext;
        this.accountService = accountService;
        this.tokenBuilderFactory = tokenBuilderFactory;
        this.tokenConfig = tokenConfig;
    }


    @Override
    public SearchResponse search(SearchRequest searchRequest) {
        checkPermission();
        // Validate filters
        if (searchRequest.getFilters() != null) {
            for (String key : searchRequest.getFilters().keySet()) {
                switch (key) {
                    case "expiresOn":
                    case "issuedOn":
                    case "updatedOn":
                        throw new BadRequestException("Filtering by date is not supported.");
                }
            }
        }
        return tokenDao.searchTokens(searchRequest);
    }

    @Override
    public Token create(final CreateTokenRequest createTokenRequest) {
        checkPermission();

        final String userId = securityContext.getUserId();

        final Optional<Integer> optionalAccountId = accountDao.getId(createTokenRequest.getUserEmail());
        final Integer accountId = optionalAccountId.orElseThrow(() ->
                new NoSuchUserException("Cannot find user to associate with this API key!"));

        // Parse and validate tokenType
        final Optional<Token.TokenType> optionalTokenType = getParsedTokenType(createTokenRequest.getTokenType());
        final Token.TokenType tokenType = optionalTokenType.orElseThrow(() -> new BadRequestException("Unknown token type:" + createTokenRequest.getTokenType()));

        final Instant expiryInstant = createTokenRequest.getExpiryDate() == null ? null : createTokenRequest.getExpiryDate().toInstant();


//        Token token = dao.createToken(
//                tokenTypeToCreate.get(),
//                userId,
//                expiryInstant,
//                createTokenRequest.getUserEmail(),
//                createTokenRequest.getClientId(),
//                createTokenRequest.isEnabled(),
//                createTokenRequest.getComments());
//
//        stroomEventLoggingService.createAction("CreateApiToken", "Create a token");
//
//
//        account.setCreateTimeMs(now);
//        account.setCreateUser(userId);
//        account.setUpdateTimeMs(now);
//        account.setUpdateUser(userId);
//        account.setFirstName(request.getFirstName());
//        account.setLastName(request.getLastName());
//        account.setEmail(request.getEmail());
//        account.setComments(request.getComments());
//        account.setForcePasswordChange(request.isForcePasswordChange());
//        account.setNeverExpires(request.isNeverExpires());
//        account.setLoginCount(0);
//        // Set enabled by default.
//        account.setEnabled(true);
//
//        id                        int(11) NOT NULL AUTO_INCREMENT,
//                version                   int(11) NOT NULL,
//        create_time_ms bigint (20) NOT NULL,
//        create_user varchar (255) NOT NULL,
//        update_time_ms bigint (20) NOT NULL,
//        update_user varchar (255) NOT NULL,
//        fk_account_id             int(11) NOT NULL,
//        fk_token_type_id          int(11) NOT NULL,
//        data longtext,
//        expires_on_ms bigint (20) DEFAULT NULL,
//        comments longtext,
//        enabled bit (1) NOT NULL,


        final long now = System.currentTimeMillis();

        final TokenBuilder tokenBuilder = tokenBuilderFactory
                .expiryDateForApiKeys(expiryInstant)
                .newBuilder(tokenType)
                .clientId(createTokenRequest.getClientId())
                .subject(createTokenRequest.getUserEmail());

        final Instant actualExpiryDate = tokenBuilder.getExpiryDate();
        final String data = tokenBuilder.build();

        final Token token = new Token();
        token.setCreateTimeMs(now);
        token.setCreateUser(userId);
        token.setUpdateTimeMs(now);
        token.setUpdateUser(userId);
        token.setUserEmail(createTokenRequest.getUserEmail());
        token.setTokenType(tokenType.getText());
        token.setData(data);
        token.setExpiresOnMs(actualExpiryDate.toEpochMilli());
        token.setComments(createTokenRequest.getComments());
        token.setEnabled(createTokenRequest.isEnabled());

//
//                token.setToken(idToken);
//        token.setTokenType(tokenTypeToCreate.get().getText());
//        token.setEnabled(createTokenRequest.isEnabled());
//        token.setExpiresOn();
//        token.setIssuedByUser();
//        token.setIssuedOn();
//        token.setUpdatedByUser();
//        token.setUpdatedOn();
//        token.setUserEmail();
//
//        token.setComments(request.getComments());
//        token.setForcePasswordChange(request.isForcePasswordChange());
//        token.setNeverExpires(request.isNeverExpires());
//        token.setCreateTimeMs(now);
//        token.setCreateUser(userId);
//        token.setUpdateTimeMs(now);
//        token.setUpdateUser(userId);
//        token.setLoginCount(0);
//        // Set enabled by default.
//        token.setEnabled(true);


        return tokenDao.create(accountId, token);
    }


    @Override
    public Token createResetEmailToken(final Account account, final String clientId) {
        final Token.TokenType tokenType = Token.TokenType.EMAIL_RESET;
        long timeToExpiryInSeconds = tokenConfig.getMinutesUntilExpirationForEmailResetToken() * 60;
        final TokenBuilder tokenBuilder = tokenBuilderFactory
                .expiryDateForApiKeys(Instant.now().plusSeconds(timeToExpiryInSeconds))
                .newBuilder(tokenType)
                .clientId(clientId);

        final Instant actualExpiryDate = tokenBuilder.getExpiryDate();
        final String idToken = tokenBuilder.build();

        final String userId = securityContext.getUserId();

        final long now = System.currentTimeMillis();
        final Token token = new Token();
        token.setCreateTimeMs(now);
        token.setUpdateTimeMs(now);
        token.setCreateUser(userId);
        token.setUpdateUser(userId);
        token.setUserEmail(account.getEmail());
        token.setTokenType(tokenType.getText().toLowerCase());
        token.setData(idToken);
        token.setExpiresOnMs(actualExpiryDate.toEpochMilli());
        token.setComments("Created for password reset");
        token.setEnabled(true);

        return tokenDao.create(account.getId(), token);
    }

    @Override
    public int deleteAll() {
        checkPermission();

        return tokenDao.deleteAllTokensExceptAdmins();
    }

    @Override
    public int delete(int tokenId) {
        checkPermission();

        return tokenDao.deleteTokenById(tokenId);
    }

    @Override
    public int delete(String token) {
        checkPermission();

        return tokenDao.deleteTokenByTokenString(token);
    }

    @Override
    public Optional<Token> read(String token) {
        checkPermission();

        return tokenDao.readByToken(token);
    }

    @Override
    public Optional<Token> read(int tokenId) {
        checkPermission();

        return tokenDao.readById(tokenId);
    }

    @Override
    public int toggleEnabled(int tokenId, boolean isEnabled) {
        checkPermission();
        final String userId = securityContext.getUserId();

        Optional<Account> updatingUser = accountService.read(userId);

        return updatingUser
                .map(account -> tokenDao.enableOrDisableToken(tokenId, isEnabled, account))
                .orElse(0);
    }

//    @Override
//    public Optional<String> verifyToken(String token) {
////        Optional<Token> tokenRecord = dao.readByToken(token);
////        if (!tokenRecord.isPresent()) {
////            return Optional.empty();
////        }
//        return tokenVerifier.verifyToken(token);
//    }

    @Override
    public String getPublicKey() {
        return jwkCache.get().get(0).toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
    }

    private void checkPermission() {
        if (!securityContext.hasAppPermission(PermissionNames.MANAGE_USERS_PERMISSION)) {
            throw new PermissionException(securityContext.getUserId(), "You do not have permission to manage users");
        }
    }

    static Optional<Token.TokenType> getParsedTokenType(String tokenType) {
        switch (tokenType.toLowerCase()) {
            case "api":
                return Optional.of(Token.TokenType.API);
            case "user":
                return Optional.of(Token.TokenType.USER);
            case "email_reset":
                return Optional.of(Token.TokenType.EMAIL_RESET);
            default:
                return Optional.empty();
        }
    }


}
