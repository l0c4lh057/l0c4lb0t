package DataManager;

import java.time.Instant;

public class SQLGuild {

    private long guildId = 0L;
    private String name = "";
    private String iconUrl = "";
    private Instant createdAt = Instant.MIN;
    private Instant joinedAt = Instant.MIN;
    private long ownerId = 0L;
    private String botPrefix = "=";
    private String language = "en";
    private long joinRole = 0L;
    private String joinMessage = "";
    private String leaveMessage = "";
    private String banMessage = "";
    private String unknownCommandMessage = "";
    private long publicChannelId = 0L;
    private boolean deleteInvites = false;
    private String inviteWarning = "";
    private int sentMessageCount = 0;
    private int receivedMessageCount = 0;
    private int receivedCommandCount = 0;
    private int receivedPublicMessageCount = 0;
    private int receivedUnknownCommandCount = 0;
    private int receivedCustomCommandCount = 0;
    private String token = "";
    private long suggestionChannelId = 0L;
    private String readonlyToken = "";

    public SQLGuild(){}
    
    public SQLGuild(long guildId, String name, String iconUrl, Instant createdAt, Instant joinedAt, long ownerId, String botPrefix, String language, long joinRole, String joinMessage, String leaveMessage, String banMessage, String unknownCommandMessage, long publicChannelId, boolean deleteInvites, String inviteWarning, int sentMessageCount, int receivedMessageCount, int receivedCommandCount, int receivedPublicMessageCount, int receivedUnknownCommandCount, int receivedCustomCommandCount, String token, long suggestionChannelId, String readonlyToken) {
        this.guildId = guildId;
        this.name = name;
        this.iconUrl = iconUrl;
        this.createdAt = createdAt;
        this.joinedAt = joinedAt;
        this.ownerId = ownerId;
        this.botPrefix = botPrefix;
        this.language = language;
        this.joinRole = joinRole;
        this.joinMessage = joinMessage;
        this.leaveMessage = leaveMessage;
        this.banMessage = banMessage;
        this.unknownCommandMessage = unknownCommandMessage;
        this.publicChannelId = publicChannelId;
        this.deleteInvites = deleteInvites;
        this.inviteWarning = inviteWarning;
        this.sentMessageCount = sentMessageCount;
        this.receivedMessageCount = receivedMessageCount;
        this.receivedCommandCount = receivedCommandCount;
        this.receivedPublicMessageCount = receivedPublicMessageCount;
        this.receivedUnknownCommandCount = receivedUnknownCommandCount;
        this.receivedCustomCommandCount = receivedCustomCommandCount;
        this.token = token;
        this.suggestionChannelId = suggestionChannelId;
        this.readonlyToken = readonlyToken;
    }

    public long getGuildId() {return guildId;}
    public String getName() {return name;}
    public Instant getCreatedAt() {return createdAt;}
    public Instant getJoinedAt() {return joinedAt;}
    public long getOwnerId() {return ownerId;}
    public String getBotPrefix(){return botPrefix;}
    public String getLanguage() {return language;}
    public long getJoinRole() {return joinRole;}
    public String getJoinMessage() {return joinMessage;}
    public String getLeaveMessage() {return leaveMessage;}
    public String getBanMessage() {return banMessage;}
    public String getUnknownCommandMessage() {return unknownCommandMessage;}
    public long getPublicChannelId() {return publicChannelId;}
    public boolean getDeleteInvites() {return deleteInvites;}
    public String getInviteWarning() {return inviteWarning;}
    public int getSentMessageCount() {return sentMessageCount;}
    public int getReceivedMessageCount() {return receivedMessageCount;}
    public int getReceivedCommandCount() {return receivedCommandCount;}
    public int getReceivedPublicMessageCount() {return receivedPublicMessageCount;}
    public int getReceivedUnknownCommandCount() {return receivedUnknownCommandCount;}
    public int getReceivedCustomCommandCount() {return receivedCustomCommandCount;}
    public String getToken() {return token;}
    public long getSuggestionChannelId() {return suggestionChannelId;}
    public String getReadonlyToken() {return readonlyToken;}

}
