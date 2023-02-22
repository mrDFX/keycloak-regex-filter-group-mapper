package groups;

import org.keycloak.models.GroupModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link org.keycloak.protocol.ProtocolMapper} that add custom optional claim containing the uuids of groups the user
 * is assigned to
 */
public class GroupFilterMapper extends GroupMembershipMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static final String GROUPS_PREFIX = "groups.prefix";

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);

        ProviderConfigProperty providerConfigProperty;

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName(GROUPS_PREFIX);
        providerConfigProperty.setLabel("Group prefix");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setDefaultValue("");
        providerConfigProperty.setHelpText("In this field, you must specify the regexp by which groups within the given REALM will be filtered; Any regular expression (PCRE) must begin with the character ^ ; Example: if there are groups with the following names: g1-a-a1 g1-a-a2 g1-b-a1 g1-b-a3 g1-c-a1 g1-c-a3 – and for this client, only groups are needed: 1. Beginning with g1-a-a 2. All groups starting with g1- and ending with -a3 - for this, a regexp of the type is suitable: ^g1-a-a.+|^g1-.+-a3.+");
        providerConfigProperty.setSecret(true);
        configProperties.add(providerConfigProperty);

        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, GroupFilterMapper.class);
    }

    public static final String PROVIDER_ID = "group-filter-mapper";

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Group Filter Membership";
    }

    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Add group membership to user (list of group names)";
    }

    /**
     * Adds the group membership information to the {@link IDToken#otherClaims}.
     * @param token The {@link IDToken}
     * @param mappingModel The {@link ProtocolMapperModel}
     * @param userSession The {@link UserSessionModel}
     */
    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        List<String> membership = userSession.getUser().getGroupsStream().map(GroupModel::getName).collect(Collectors.toList());
        String protocolClaim = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        List<String> groups = new ArrayList<String>();
        String prefix = mappingModel.getConfig().get(GROUPS_PREFIX);

        String groupName;
        for (int i = 0; i < membership.size(); i++) {
            groupName = membership.get(i);
            if (groupName.matches(prefix)) {
                groups.add(groupName);
            }
        }
        token.getOtherClaims().put(protocolClaim, groups);
    }

    /**
     * Create {@link ProtocolMapperModel}
     * @param name  The name
     * @param tokenClaimName The tokenClaimName
     * @param consentRequired is consentRequired
     * @param consentText The consentText
     * @param accessToken include in access token
     * @param idToken include in ID token
     * @return The {@link ProtocolMapperModel}
     */
    public static ProtocolMapperModel create(String name,
                                             String tokenClaimName,
                                             boolean consentRequired, String consentText,
                                             boolean accessToken, boolean idToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "groups_tmp");
        config.put(GROUPS_PREFIX, "");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);

        return mapper;
    }
}
