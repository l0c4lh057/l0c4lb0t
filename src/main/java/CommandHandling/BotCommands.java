package CommandHandling;

import DataManager.*;
import DataManager.DataManager.*;
import Main.*;
import Media.NSFWUtils;
import Media.SFWUtils;
import Media.UrbanDictionary;
import Media.Wikipedia;
import Music.MusicManager;
import Patreon.PatreonManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.voice.VoiceConnection;
import org.apache.commons.io.IOUtils;
import org.mariuszgromada.math.mxparser.Expression;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.JDBCType;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BotCommands {

    /**
     * The id list of the guilds that have automatic backups enabled
     */
    public static List<Long> autoBackupGuilds = new ArrayList<>();

    public static void registerCommands(){
        new Command("ping", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createMessage("pong"))
                .thenReturn(true)
                )
                .withRatelimit(new RatelimitUtils.Ratelimit(5, 20000))
                .withPermissions("ping")
                .usableByEveryone(true)
                .register();
        new Command("test2", (e, prefix, args, lang) -> e.getMessage().getChannel()
                //.filter(c -> !RatelimitUtils.isMemberRateLimited(e.getGuildId().map(Snowflake::asLong).orElseThrow(), e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow(), RatelimitUtils.RatelimitChannel.TEST, 5, 10_000, c, lang))
                .flatMap(c -> c.createMessage("Args: " + args.toString()))
                .thenReturn(true)
                )
                .withPermissions("test2")
                .usableByEveryone(true)
                .register();
        new Command("prefix", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    if(args.size() < 2){
                        return false;
                    }
                    if(args.get(0).equalsIgnoreCase("set")){
                        String newPref = String.join(" ", args.subList(1, args.size())).trim();
                        if(newPref.length() == 0 || newPref.length() > 20){
                            return false;
                        }
                        if(DataManager.setGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow(), "bot_prefix", newPref, JDBCType.VARCHAR))
                            c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.prefix.set", newPref)).subscribe();
                        else
                            return BotUtils.sendErrorMessage(c);
                        return true;
                    }
                    return false;
                }))
                .withAliases("pref")
                .withPermissions("prefix", Permission.ADMINISTRATOR)
                //.usableInDM(true)
                .register();
        new Command("language", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> args.size() == 1 || args.size() == 2)
                .map(c -> {
                    if(args.size() == 1 && args.get(0).equalsIgnoreCase("get")){
                        c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.language.get", LocaleManager.getAvailableLanguages().values().stream().map(s -> s[0]).collect(Collectors.joining(", ")))).subscribe();
                        return true;
                    }else if(args.size() == 2 && args.get(0).equalsIgnoreCase("set")){
                        String l = LocaleManager.getLanguage(args.get(1));
                        if(l == null)
                            c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.language.notFound", args.get(1), prefix)).subscribe();
                        else{
                            if(e.getGuildId().isPresent()){
                                if(DataManager.setGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow(), "language", l, JDBCType.VARCHAR))
                                    c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.language.set", LocaleManager.getAvailableLanguages().get(l)[0])).subscribe();
                                else
                                    BotUtils.sendErrorMessage(c);
                            }else{
                                if(DataManager.setUser(e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow(), "language", l, JDBCType.VARCHAR))
                                    // TODO: implement dm version of this method when users are saved
                                    ;//c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.language.set", LocaleManager.getAvailableLanguages().get(l)[0])).subscribe();
                                else
                                    BotUtils.sendErrorMessage(c);
                            }
                        }
                        return true;
                    }
                    return false;
                }))
                .withAliases("lang")
                .withPermissions("language", Permission.ADMINISTRATOR)
                .usableInDM(true)
                .register();
        new Command("choose", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> args.size() > 1)
                .flatMap(c -> {
                    Random rn = new Random();
                    return c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.choose.chosen", args.get(rn.nextInt(args.size())))).map(x -> true);
                }))
                .withAliases("c")
                .withPermissions("choose")
                .usableByEveryone(true)
                .usableInDM(true)
                .register();
        new Command("userlimit")
                .withPermissions("userlimit")
                .usableByEveryone(true)
                .register();
        new Command("token", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    if(args.size() != 1 && args.size() != 2)
                        return false;
                    if(args.get(0).equalsIgnoreCase("get") && args.size() == 1){
                        try{
                            SQLGuild g = DataManager.getGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            e.getMessage().getAuthorAsMember().flatMap(User::getPrivateChannel)
                                    .flatMap(pc -> pc.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.token.get.dm", g.getToken(), g.getReadonlyToken())))
                                    .subscribe();
                            c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.token.get.guild")).subscribe();
                        }catch(Exception ex){
                            ex.printStackTrace();
                            BotUtils.sendErrorMessage(c);
                        }
                        return true;
                    }else if((args.get(0).equalsIgnoreCase("new") || args.get(0).equalsIgnoreCase("renew")) && args.size() == 2){
                        String newToken = "";
                        boolean edit = false;
                        if(args.get(1).equalsIgnoreCase("edit")){
                            newToken = DataManager.renewToken(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            edit = true;
                        }else if(args.get(1).equalsIgnoreCase("readonly")){
                            newToken = DataManager.renewReadonlyToken(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                        }
                        if(newToken == null)
                            BotUtils.sendErrorMessage(c);
                        else if(newToken.length() == 0)
                            return false;
                        else{
                            c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.token.new." + (edit ? "edit" : "readonly") + ".guild")).subscribe();
                            Consumer<EmbedCreateSpec> ecs = LocaleManager.getLanguageMessage(lang, "commands.token.new." + (edit ? "edit" : "readonly") + ".dm", newToken);
                            e.getMessage().getAuthorAsMember().flatMap(User::getPrivateChannel)
                                    .flatMap(pc -> pc.createEmbed(ecs))
                                    .subscribe();
                        }
                        return true;
                    }
                    return false;
                }))
                .requiresBotOwner(true)
                .register();
        /*new Command("weather", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    if(args.isEmpty())
                        return false;
                    String query = String.join(" ", args);
                    boolean metric = true;
                    JsonNode weather = Weather.getWeather(query, lang, metric);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    if(weather == null || query.toLowerCase().contains("bielefeld")){
                        c.createMessage("Could not find a city with the name `" + query + "`").subscribe();
                    }else {
                        c.createMessage(mcs -> mcs.setEmbed(ecs -> {
                            ecs.setTitle(weather.get("name").asText() + ", " + weather.get("sys").get("country").asText());
                            ecs.setThumbnail(Weather.getImageUrl(weather.get("weather").get(0).get("icon").asText()));
                            ecs.setDescription(weather.get("weather").get(0).get("main").asText() + ": " + weather.get("weather").get(0).get("description").asText());
                            ecs.addField("Main", "Temperature: " + weather.get("main").get("temp").asDouble() + "°" + (metric ? "C" : "F") + "\n- min: " + weather.get("main").get("temp_min").asDouble() + "°" + (metric ? "C" : "F") + "\n- max: " + weather.get("main").get("temp_max").asDouble() + "°" + (metric ? "C" : "F") + "\nAir pressure: " + weather.get("main").get("pressure").asInt() + "hPa\nHumidity: " + weather.get("main").get("humidity").asInt() + "%", false);
                            ecs.addField("Sun", "Sunrise: " + format.format(new Date(weather.get("sys").get("sunrise").asLong() * 1000)) + "\nSunset: " + format.format(new Date(weather.get("sys").get("sunset").asLong() * 1000)), false);
                            ecs.addField("Clouds", "Cloudiness: " + weather.get("clouds").get("all").asInt() + "%", false);
                            ecs.addField("Wind", "Speed: " + weather.get("wind").get("speed").asDouble() + (metric ? "m/s" : "mph") + "\nAngle: " + weather.get("wind").get("deg").asInt() + "°" + (weather.get("wind").has("gust") ? "\nGust: " + weather.get("wind").get("gust").asDouble() + (metric ? "m/s" : "mph") : ""), false);
                            ecs.setFooter("Lon: " + weather.get("coord").get("lon").asDouble() + ", Lat: " + weather.get("coord").get("lat").asDouble(), null);
                        })).subscribe();
                    }
                    return true;
                })
        ));*/
        new Command("dynamicvoicechannel", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("list")){
                            c.createMessage("" + DataManager.getDVCs(e.getGuildId().map(Snowflake::asLong).orElseThrow())).subscribe();
                            return true;
                        }
                    }else if(args.size() > 1){
                        if(args.get(0).equalsIgnoreCase("add")){
                            String name = String.join(" ", args.subList(1, args.size()));
                            if(DataManager.isDVC(e.getGuildId().map(Snowflake::asLong).orElseThrow(), name, null))
                                c.createMessage("`" + name + "` is already a dvc").subscribe();
                            else if(DataManager.addDVC(e.getGuildId().map(Snowflake::asLong).orElseThrow(), name))
                                c.createMessage("`" + name + "` is now a dvc").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return true;
                        }else if(args.get(0).equalsIgnoreCase("remove")){
                            String name = String.join(" ", args.subList(1, args.size()));
                            if(!DataManager.isDVC(e.getGuildId().map(Snowflake::asLong).orElseThrow(), name, null))
                                c.createMessage("`" + name + "` is not a dvc").subscribe();
                            else if(DataManager.removeDVC(e.getGuildId().map(Snowflake::asLong).orElseThrow(), name))
                                c.createMessage("`" + name + "` is no longer a dvc").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return true;
                        }
                    }
                    return false;
                }))
                .withAliases("dvc")
                .withPermissions("manageDVCs", Permission.MANAGE_CHANNELS)
                .register();
        new Command("poll", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    if(args.size() < 4)
                        return false;
                    long duration = BotUtils.getPollDuration(args.get(0));
                    if(duration < 0)
                        return false;
                    args.remove(0);
                    AtomicBoolean multiVote = new AtomicBoolean(false);
                    List<String> emojis = new ArrayList<>();
                    for(int i = 1; i < 10; i++) emojis.add(i + "\u20E3");
                    emojis.add("\uD83D\uDD1F");
                    if(args.get(0).equalsIgnoreCase("yn")){
                        if(args.size() > 5)
                            return false;
                        args.remove(0);
                        emojis.clear();
                        emojis.addAll(new ArrayList<>(Arrays.asList("\u2705", BotUtils.x.asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw).get(), "*\u20E3")));
                    }else if(args.get(0).equalsIgnoreCase("multi")){
                        multiVote.set(true);
                        args.remove(0);
                    }
                    String title = args.get(0);
                    args.remove(0);
                    String description = args.get(0);
                    args.remove(0);
                    if(args.size() > 20)
                        return false;
                    if(args.size() > 10){
                        emojis.clear();
                        for(int i = 0; i < 20; i++)
                            emojis.add("\uD83C" + (char)(i + 56806));
                    }

                    StringBuilder values = new StringBuilder();
                    for(int i = 0; i < args.size(); i++)
                        values.append("\n").append(emojis.get(i)).append(" ").append(args.get(i));

                    /*Message m = c.createMessage(mcs -> mcs.setEmbed(ecs -> {
                        ecs.setTitle(BotUtils.formatString("**Poll** _({0})_", BotUtils.getDuration(duration)));
                        ecs.setDescription("**" + title.replace("*", "\\*") + "**" + values.toString());
                        ecs.setFooter(multiVote.get() ? "Multiple votes per user" : "One vote per user", null);
                        ecs.setTimestamp(Instant.now().plusSeconds(duration));
                    })).block();
                    for(int i = 0; i < args.size(); i++)
                        m.addReaction(emojis.get(i)).subscribe();*/
                    AtomicInteger cnt = new AtomicInteger(0);
                    c.createMessage(mcs ->
                            mcs.setEmbed(ecs -> {
                                ecs.setTitle(BotUtils.formatString("**Poll** _({0})_", BotUtils.getDuration(duration)));
                                if(description.replace("*", "\\*").length() + values.toString().length() <= 1024)
                                    ecs.addField(title, "**" + description.replace("*", "\\*") + "**" + values.toString(), false);
                                else
                                    ecs.setDescription("**" + title.replace("*", "\\*") + "**\n" + description + values.toString());
                                ecs.setFooter(multiVote.get() ? "Multiple votes per user" : "One vote per user", null);
                                ecs.setTimestamp(Instant.now().plusSeconds(duration));
                            }))
                            .flatMap(m -> Flux.fromIterable(emojis)
                                    .filter(emoji -> cnt.getAndIncrement() < args.size())
                                    .map(ReactionEmoji::unicode)
                                    .flatMap(em -> m.addReaction(em))
                                    .next()
                            )
                            .subscribe();
                    return true;
                }))
                .withPermissions("createPoll")
                .usableByEveryone(true)
                .register();
        new Command("invites", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("get")){
                            SQLGuild sg = DataManager.getGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            if(sg != null)
                                c.createMessage("delete invites: " + sg.isDeleteInvites() + "\nwarning: " + sg.getInviteWarning()).subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return true;
                        }else if(args.get(0).equalsIgnoreCase("whitelist")){
                            List<String> allowed = DataManager.getAllowedInvites(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            c.createMessage("Allowed invites: " + allowed).subscribe();
                        }
                    }else if(args.size() == 2){
                        if(args.get(0).equalsIgnoreCase("delete")){
                            boolean block = Boolean.parseBoolean(args.get(1));
                            if(DataManager.setGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow(), "delete_invites", block, JDBCType.BOOLEAN)){
                                c.createMessage("set to " + block).subscribe();
                            }else{
                                BotUtils.sendErrorMessage(c);
                            }
                            return true;
                        }else if(args.get(0).equalsIgnoreCase("allow")){
                            String invite = args.get(1);
                            Matcher m = Pattern.compile(BotUtils.INVITE_MATCHER).matcher(invite);
                            if(m.find()) invite = m.group();
                            if(DataManager.isInviteAllowed(e.getGuildId().map(Snowflake::asLong).orElseThrow(), invite))
                                c.createMessage("this invite is already allowed").subscribe();
                            else if(DataManager.allowInvite(e.getGuildId().map(Snowflake::asLong).orElseThrow(), invite))
                                c.createMessage("invite " + invite + " is now allowed").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return true;
                        }else if(args.get(0).equalsIgnoreCase("disallow")){
                            String invite = args.get(1);
                            Matcher m = Pattern.compile(BotUtils.INVITE_MATCHER).matcher(invite);
                            if(m.find()) invite = m.group();
                            if(!DataManager.isInviteAllowed(e.getGuildId().map(Snowflake::asLong).orElseThrow(), invite))
                                c.createMessage("this invite is not even allowed...").subscribe();
                            else if(DataManager.disallowInvite(e.getGuildId().map(Snowflake::asLong).orElseThrow(), invite))
                                c.createMessage("invite " + invite + " is not allowed anymore").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return true;
                        }
                    }
                    if(args.size() > 1){
                        if(args.get(0).equalsIgnoreCase("warning")){
                            if(args.size() == 2 && (args.get(1).equalsIgnoreCase("disable") || args.get(1).equalsIgnoreCase("remove"))){
                                if(!DataManager.setGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow(), "invite_warning", "", JDBCType.VARCHAR))
                                    BotUtils.sendErrorMessage(c);
                                else
                                    c.createMessage("removed warning").subscribe();
                            }else if(args.size() > 2 && args.get(1).equalsIgnoreCase("set")){
                                String warning = String.join(" ", args.subList(2, args.size()));
                                if(!DataManager.setGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow(), "invite_warning", warning, JDBCType.VARCHAR))
                                    BotUtils.sendErrorMessage(c);
                                else
                                    c.createMessage("new warning: " + warning).subscribe();
                            }
                            return true;
                        }
                    }
                    return false;
                }))
                .withAliases("invite")
                .withPermissions("blockInvites", Permission.MANAGE_MESSAGES)
                .register();
        new Command("permissions")
                .withAliases("perms")
                .withPermissions("permissions", Permission.ADMINISTRATOR)
                .register();
        new Command("script", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    /*if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("upload") || args.get(0).equalsIgnoreCase("save")){
                            if(e.getMessage().getAttachments().size() == 1){
                                Attachment at = e.getMessage().getAttachments().iterator().next();
                                if(at.getFilename().endsWith(".lbs") && at.getFilename().contains("-")){
                                    List<String> tiles = new ArrayList<>(Arrays.asList(at.getFilename().split("-")));
                                    String eventType = tiles.get(0);
                                    String scriptName = String.join("-", tiles.subList(1, tiles.size()));
                                    ScriptExecutor.ScriptEvent event = ScriptExecutor.ScriptEvent.getEvent(scriptName);
                                    if(event != null){
                                        try {
                                            Connection con = Jsoup.connect(at.getUrl()).header("Authorization", "Bot " + Tokens.BOT_TOKEN).header("User-Agent", "l0c4lb0t (Discord4J) made by l0c4lh057 - Downloading script");
                                            Document doc = con.get();
                                            doc.outputSettings().prettyPrint(false);
                                            String scriptContent = doc.body().html();
                                            if(DataManager.addScript(e.getGuildId().map(Snowflake::asLong).orElseThrow()), event, scriptName, scriptContent))
                                                c.createMessage("successfully uploaded script").subscribe();
                                            else
                                                return BotUtils.sendErrorMessage(c);
                                        }catch (IOException ex){
                                            ex.printStackTrace();
                                            return BotUtils.sendErrorMessage(c);
                                        }
                                        return true;
                                    }else{
                                        c.createMessage("unknown event " + eventType).subscribe();
                                        return true;
                                    }
                                }
                            }
                        }
                    }*/
                    return true;
                }))
                .withAliases("scripts")
                .withPermissions("manageScripts", Permission.ADMINISTRATOR)
                .register();
        new Command("command", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("list")){
                            Map<String, String> cc = DataManager.getCustomCommands(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            if(cc.isEmpty())
                                c.createMessage("no custom commands here").subscribe();
                            else
                                c.createMessage("custom commands: " + String.join(", ", cc.keySet())).subscribe();
                            return true;
                        }
                    }else if(args.size() == 2){
                        if(args.get(0).equalsIgnoreCase("get")){
                            String cmd = args.get(1);
                            Map<String, String> cc = DataManager.getCustomCommands(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            if(cc.containsKey(cmd))
                                c.createMessage("CommandExecutable: `" + cmd + "`\nResponse: ```\n" + cc.get(cmd) + "```").subscribe();
                            else
                                c.createMessage("command `" + cmd + "` does not exist").subscribe();
                            return true;
                        }else if(args.get(0).equalsIgnoreCase("remove") || args.get(0).equalsIgnoreCase("delete")){
                            String cmd = args.get(1);
                            Map<String, String> cc = DataManager.getCustomCommands(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            if(cc.containsKey(cmd)){
                                if(DataManager.removeCustomCommand(e.getGuildId().map(Snowflake::asLong).orElseThrow(), cmd))
                                    c.createMessage("removed custom command").subscribe();
                                else
                                    BotUtils.sendErrorMessage(c);
                            }else
                                c.createMessage("this custom command does not exist").subscribe();
                            return true;
                        }
                    }else if(args.size() > 2){
                        if(args.get(0).equalsIgnoreCase("add") || args.get(0).equalsIgnoreCase("create")){
                            String cmd = args.get(1);
                            String response = String.join(" ", args.subList(2, args.size()));
                            Map<String, String> cc = DataManager.getCustomCommands(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            if(cc.containsKey(cmd))
                                c.createMessage("command already exists").subscribe();
                            else if(DataManager.addCustomCommand(e.getGuildId().map(Snowflake::asLong).orElseThrow(), cmd, response))
                                c.createMessage("added custom command").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return true;
                        }
                    }
                    return false;
                }))
                .withAliases("commands", "cmds")
                .withPermissions("manageCustomCommands", Permission.MANAGE_MESSAGES)
                .register();
        new Command("blockchannel", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("list")){
                            List<Long> blocked = DataManager.getBlockedChannels(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            c.createMessage("Blocked channels: " + blocked.stream().map(cId -> "<#" + cId + ">").collect(Collectors.joining(", "))).subscribe();
                            return true;
                        }
                    }else if(args.size() == 2){
                        if(args.get(0).equalsIgnoreCase("add")){
                            GuildMessageChannel bc = BotUtils.getChannelFromArgument(e.getGuild(), args.get(1)).ofType(GuildMessageChannel.class).block();
                            if(bc == null) return false;
                            List<Long> blocked = DataManager.getBlockedChannels(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            if(blocked.contains(bc.getId().asLong()))
                                c.createMessage("already blocked").subscribe();
                            else if(DataManager.addBlockedChannel(e.getGuildId().map(Snowflake::asLong).orElseThrow(), bc.getId().asLong()))
                                c.createMessage("blocked channel").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return true;
                        }else if(args.get(0).equalsIgnoreCase("remove")){
                            List<Long> blocked = DataManager.getBlockedChannels(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            GuildMessageChannel bc = BotUtils.getChannelFromArgument(e.getGuild(), args.get(1)).ofType(GuildMessageChannel.class).block();
                            if(bc == null) return false;
                            if(!blocked.contains(bc.getId().asLong()))
                                c.createMessage("not blocked").subscribe();
                            else if(DataManager.removeBlockedChannel(e.getGuildId().map(Snowflake::asLong).orElseThrow(), bc.getId().asLong()))
                                c.createMessage("unblocked channel").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return true;
                        }
                    }
                    return false;
                }))
                .withAliases("bc")
                .withPermissions("blockChannel", Permission.MANAGE_CHANNELS)
                .register();
        new Command("unknowncommandmessage", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("get")){
                            String ucm = DataManager.getGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow()).getUnknownCommandMessage();
                            if(ucm.length() == 0)
                                c.createMessage("no ucm").subscribe();
                            else
                                c.createMessage("ucm: " + ucm).subscribe();
                            return true;
                        }else if(args.get(0).equalsIgnoreCase("remove") || args.get(0).equalsIgnoreCase("delete")){
                            if(DataManager.setGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow(), "unknown_command_message", "", JDBCType.VARCHAR))
                                c.createMessage("removed ucm").subscribe();
                            else
                                return BotUtils.sendErrorMessage(c);
                            return true;
                        }
                    }else if(args.size() > 1){
                        if(args.get(0).equalsIgnoreCase("set")){
                            String newUcm = String.join(" ", args.subList(1, args.size()));
                            if(DataManager.setGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow(), "unknown_command_message", newUcm, JDBCType.VARCHAR))
                                c.createMessage("Changed ucm to " + newUcm).subscribe();
                            else
                                return BotUtils.sendErrorMessage(c);
                            return true;
                        }
                    }
                    return false;
                }))
                .withAliases("ucm")
                .withPermissions("unknownCommandMessage", Permission.MANAGE_MESSAGES)
                .register();
        new Command("joinrole").register();
        new Command("joinmessage").register();
        new Command("leavemessage").register();
        new Command("banmessage").register();
        //new Command("publicchannel").register();
        new Command("report").register();
        new Command("calc", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> args.size() > 0)
                .flatMap(c -> Mono.just(new Expression(String.join(" ", args)))
                        .flatMap(expression -> {
                            double result = expression.calculate();
                            if(Double.isNaN(result))
                                return c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.calc.error", "" + expression.getErrorMessage()));
                            else
                                return c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.calc.result", expression.getExpressionString(), "" + result, "" + (int)(expression.getComputingTime() * 1000)));
                        })
                        .map(x -> true)
                ))
                .withAliases("solve", "calculate")
                .withPermissions("calculate")
                .usableByEveryone(true)
                .usableInDM(true)
                .register();

        /* GENERAL */

        new Command("help", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    AtomicInteger pageNbr = new AtomicInteger(0);
                    if(args.size() > 0) try{
                        if(args.get(0).matches("^\\d+$"))
                            pageNbr.set(Integer.parseInt(args.get(0)) - 1);
                        else{
                            if(LocaleManager.getLanguageElement(lang, "commands").has(args.get(0))){
                                BotUtils.sendHelpMessage(c, args.get(0), prefix, lang);
                                return true;
                            }
                            Optional<Command> cmd = Command.getCommand(args.get(0));
                            if(cmd.isPresent()){
                                BotUtils.sendHelpMessage(c, cmd.get().getName(), prefix, lang);
                                return true;
                            }
                        }
                    }catch(NumberFormatException ignored){
                    }
                    Map<String, Map<String, String>> pages = BotUtils.getHelpPages(e.getGuild().block());
                    pageNbr.set(BotUtils.clamp(pageNbr.get(), 0, pages.size() - 1));
                    int currPage = -1;
                    for(String pageName : pages.keySet()){
                        currPage++;
                        if(pageNbr.get() != currPage) continue;
                        Map<String, String> page = pages.get(pageName);
                        c.createEmbed(ecs -> ecs
                                .setTitle(LocaleManager.getLanguageString(lang, "help.title"))
                                .addField(pageName, page.values().stream().map(cmd -> BotUtils.formatString(cmd, prefix)).collect(Collectors.joining("\n")), false)
                                .setFooter(LocaleManager.getLanguageString(lang, "help.footer", "" + (pageNbr.get() + 1), "" + pages.size()), null)
                                .setColor(BotUtils.botColor)
                        ).flatMap(m -> Flux.fromIterable(Arrays.asList(BotUtils.arrowLeft, BotUtils.arrowRight, BotUtils.x))
                                .flatMap(em -> m.addReaction(em))
                                .next()
                        ).subscribe();
                        break;
                    }
                    return true;
                }))
                .usableInDM(true)
                .usableByEveryone(true)
                .register();
        new Command("about", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createEmbed(ecs -> ecs
                        .setTitle(LocaleManager.getLanguageString(lang, "commands.about.info.title"))
                        .setDescription(LocaleManager.getLanguageString(lang, "commands.about.info.content",
                                prefix,
                                BotUtils.l0c4lh057.getUsername() + "#" + BotUtils.l0c4lh057.getDiscriminator()
                        ))
                        .setColor(BotUtils.botColor)
                ))
                .map(x -> true)
                )
                .withAliases("l0c4lb0t")
                .usableByEveryone(true)
                .usableInDM(true)
                .register();
        new Command("stats", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> e.getClient().getGuilds().count()
                        .flatMap(guildCount -> e.getClient().getGuilds().flatMap(Guild::getMembers).count()
                                .flatMap(memberCount -> e.getClient().getGuilds().flatMap(Guild::getMembers).map(User::getId).distinct().count()
                                        .flatMap(distinctMemberCount -> e.getGuild().flatMapMany(Guild::getMembers).count()
                                                .flatMap(guildMemberCount -> Mono.just(DataManager.getBotStats())
                                                        .flatMap(stats -> {
                                                            if(e.getGuildId().isPresent()){
                                                                return Mono.just(DataManager.getGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow())).flatMap(g -> c.createEmbed(ecs -> LocaleManager.addEmbedFields(lang, ecs, "commands.stats.guild.fields",
                                                                                "" + guildCount,
                                                                                "" + memberCount,
                                                                                "" + distinctMemberCount,
                                                                                "" + stats.getSentMessageCount(),
                                                                                "" + stats.getReceivedMessageCount(),
                                                                                "" + stats.getSentDMCount(),
                                                                                "" + stats.getReceivedDMCount(),
                                                                                "" + stats.getReceivedCommandCount(),
                                                                                "" + stats.getReceivedUnknownCommandCount(),
                                                                                "" + stats.getReceivedCustomCommandCount(),
                                                                                "" + guildMemberCount,
                                                                                "" + g.getSentMessageCount(),
                                                                                "" + g.getReceivedMessageCount(),
                                                                                "" + g.getReceivedCommandCount(),
                                                                                "" + g.getReceivedUnknownCommandCount(),
                                                                                "" + g.getReceivedCustomCommandCount()
                                                                        )
                                                                        .setTitle(LocaleManager.getLanguageString(lang, "commands.stats.title"))
                                                                        .setColor(BotUtils.botColor)
                                                                ));
                                                            }else{
                                                                return c.createEmbed(ecs -> ecs
                                                                        .setTitle(LocaleManager.getLanguageString(lang, "commands.stats.title"))
                                                                        .setDescription(LocaleManager.getLanguageString(lang, "commands.stats.dm.content",
                                                                                "" + guildCount,
                                                                                "" + memberCount,
                                                                                "" + distinctMemberCount,
                                                                                "" + stats.getSentMessageCount(),
                                                                                "" + stats.getReceivedMessageCount(),
                                                                                "" + stats.getSentDMCount(),
                                                                                "" + stats.getReceivedDMCount(),
                                                                                "" + stats.getReceivedCommandCount(),
                                                                                "" + stats.getReceivedUnknownCommandCount(),
                                                                                "" + stats.getReceivedCustomCommandCount())
                                                                        )
                                                                        .setColor(BotUtils.botColor)
                                                                );
                                                            }
                                                        })
                                                )
                                        )
                                )
                        )
                )
                .map(x -> true)
                )
                .usableByEveryone(true)
                .usableInDM(true)
                .register();

        /* MODERATION */

        new Command("resetnicks", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> e.getGuild()
                        .flatMapMany(Guild::getMembers)
                        .filter(m -> m.getNickname().isPresent())
                        .flatMap(m -> m.edit(gmes -> gmes.setNickname(null))
                                .thenReturn(true)
                        )
                        .count()
                        .flatMap(cnt -> c.createMessage("Reset " + cnt + " nicknames"))
                        .map(x -> true)
                ))
                .withAliases("nickreset")
                .withPermissions("resetNicks", Permission.MANAGE_NICKNAMES)
                .register();
        new Command("reactionroles")
                .withAliases("rr")
                .register();
        new Command("ban", (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(GuildMessageChannel.class)
                .filter(c -> !args.isEmpty())
                .flatMap(c -> BotUtils.getUserFromArgument(args.get(0))
                        .flatMap(u -> c.getGuild()
                                .flatMap(g -> g.ban(u.getId(), bqs -> bqs.setDeleteMessageDays(0).setReason(e.getMember().map(Member::getId).map(Snowflake::asString).orElseThrow() + " performed ban: " + (args.size() > 1 ? String.join(" ", args.subList(1, args.size())) : null))))
                                .then(c.createMessage("banned user " + u.getUsername() + "#" + u.getDiscriminator()))
                        )
                        .switchIfEmpty(c.createMessage("could not find user"))
                        .thenReturn(true)
                ))
                .withPermissions("ban", Permission.BAN_MEMBERS)
                .register();
        new Command("kick", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> !args.isEmpty())
                .flatMap(c -> BotUtils.getMemberFromArgument(e.getGuild(), args.get(0))
                        .flatMap(m -> m.kick(e.getMember().map(Member::getId).map(Snowflake::asString).orElseThrow() + " performed kick: " + (args.size() > 1 ? String.join(" ", args.subList(1, args.size())) : ""))
                                .then(c.createMessage("kicked user " + m.getUsername() + "#" + m.getDiscriminator()))
                        )
                        .switchIfEmpty(c.createMessage("could not find user " + args.get(0)))
                        .thenReturn(true)
                ))
                .withPermissions("kick", Permission.KICK_MEMBERS)
                .register();
        new Command("unban", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> !args.isEmpty())
                .flatMap(c -> BotUtils.getUserFromArgument(args.get(0))
                        .flatMap(u -> e.getGuild()
                                .flatMap(g -> g.getBan(u.getId()).onErrorResume(err -> Mono.empty())
                                        .flatMap(b -> g.unban(u.getId(), e.getMember().map(Member::getId).map(Snowflake::asString).orElseThrow() + " performed unban: " + (args.size() > 1 ? String.join(" ", args.subList(1, args.size())) : ""))
                                                .then(c.createMessage("unbanned"))
                                        )
                                )
                                .switchIfEmpty(c.createMessage("not banned"))
                        )
                        .switchIfEmpty(c.createMessage("could not find user"))
                        .thenReturn(true)
                ))
                .withPermissions("unban", Permission.BAN_MEMBERS)
                .register();
        new Command("anticaps", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> !args.isEmpty())
                .map(c -> {
                    if(args.size() == 1){

                    }else if(args.size() == 2){

                    }else if(args.size() > 2){

                    }
                    return false;
                }))
                .withAliases("nocaps")
                .withPermissions("setAntiCaps", Permission.MANAGE_MESSAGES)
                .register();

        /* FUN */

        new Command("coinflip", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.coinflip." + (new Random().nextBoolean() ? "heads" : "tails") )))
                .map(x -> true)
                )
                .withAliases("coin")
                .withPermissions("coinflip")
                .usableByEveryone(true)
                .usableInDM(true)
                .register();
        new Command("dice", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.dice.result", ""+(new Random().nextInt(6)+1))))
                .map(x -> true)
                )
                .withPermissions("dice")
                .usableByEveryone(true)
                .usableInDM(true)
                .register();
        new Command("minesweeper").register();
        new Command("randomnumber").register();
        new Command("remind", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> args.size() > 1)
                .map(c -> {
                    long duration = BotUtils.getDuration(args.get(0));
                    if(duration == -1) return false;
                    if(duration < 60_000 || duration > 60_000 * 60 * 48){
                        c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.remind.invalidDuration", BotUtils.getDuration(duration / 1000))).subscribe();
                        return true;
                    }
                    args.remove(0);
                    String reason = String.join(" ", args);
                    Mono.delay(Duration.ofMillis(duration))
                            .flatMap(x -> c.createMessage(mcs -> mcs.setContent(e.getMessage().getAuthor().map(User::getMention).orElse("")).setEmbed(LocaleManager.getLanguageMessage(lang, "commands.remind.remind", reason))))
                            .subscribe();
                    c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.remind.set", BotUtils.getDuration(duration / 1000), reason)).subscribe();
                    return true;
                }))
                .withPermissions("setReminder")
                .usableByEveryone(true)
                .usableInDM(true)
                .register();
        new Command("cat", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createMessage(SFWUtils.getCat()))
                .map(x -> true)
                )
                .withPermissions("cat")
                .usableInDM(true)
                .usableByEveryone(true)
                .register();
        new Command("dog", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createMessage(SFWUtils.getDog()))
                .map(x -> true)
                )
                .withPermissions("dog")
                .usableByEveryone(true)
                .usableInDM(true)
                .register();

        /* MUSIC COMMANDS */

        new Command("join", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> Mono.justOrEmpty(e.getMember())
                        .flatMap(m -> m.getVoiceState()
                                .flatMap(vs -> vs.getChannel()
                                        .flatMap(vc -> vc.join(vcjs -> vcjs.setProvider(MusicManager.getProvider()))
                                                .doOnNext(vcon -> MusicManager.setGuildConnection(e.getGuildId().map(Snowflake::asLong).orElseThrow(), vcon))
                                        )
                                )
                        )
                )
                .map(x -> true)
                )
                .withPermissions("join")
                .usableByEveryone(true)
                .register();
        new Command("stop", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> Mono.justOrEmpty(MusicManager.getGuildConnection(e.getGuildId().map(Snowflake::asLong).orElseThrow()))
                        .doOnNext(VoiceConnection::disconnect)
                )
                .map(x -> true)
                )
                .withPermissions("stop")
                .usableByEveryone(true)
                .register();
        new Command("play").register();
        new Command("skip").register();
        new Command("search").register();
        new Command("queue").register();
        new Command("remove").register();
        new Command("nowplaying")
                .withAliases("np")
                .register();
        new Command("volume").register();
        new Command("pause").register();
        new Command("resume").register();
        new Command("seek").register();
        new Command("playlist")
                .withAliases("playlists")
                .register();
        new Command("loop").register();
        new Command("playingmessages")
                .withAliases("pm")
                .register();

        /* BOT STAFF ONLY */

        new Command("shutdown", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .map(c -> {
                    e.getClient().logout().block();
                    System.exit(0);
                    return true;
                }))
                .withAliases("exit").requiresBotOwner(true)
                .register();
        //new Command("getid", (e, prefix, args, lang) -> Mono.just(true), true));
        //new Command("delpm", (e, prefix, args, lang) -> Mono.just(true), true));
        //new Command("pcban", (e, prefix, args, lang) -> Mono.just(true), true));
        new Command("botban").requiresBotOwner(true).register();
        new Command("guildban").requiresBotOwner(true).register();
        //new Command("disablepc").requiresBotOwner(true));
        new Command("bcall", (e, prefix, args, lang) -> e.getMessage().getChannel()
                        .filter(c -> !args.isEmpty())
                        .map(c -> {
                            Consumer<MessageCreateSpec> mcsc = BotUtils.jsonToMessage(String.join(" ", args));
                            c.createMessage(mcsc)
                                    .doOnNext(m -> {
                                        AtomicReference<Disposable> d = new AtomicReference<>();
                                        d.set(e.getClient().getEventDispatcher().on(ReactionAddEvent.class)
                                                .filter(re -> re.getUserId().asLong() == e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElse(0L))
                                                .map(ReactionAddEvent::getEmoji)
                                                .map(ReactionEmoji::asUnicodeEmoji)
                                                .filter(Optional::isPresent)
                                                .map(Optional::get)
                                                .doOnNext(em -> {
                                                    if(em.equals(BotUtils.checkmark)){
                                                        c.createMessage("sending")
                                                                .flatMap(msg -> e.getClient().getGuilds()
                                                                        .flatMap(g -> g.getChannels()
                                                                                .ofType(TextChannel.class)
                                                                                .filterWhen(ch -> ch.getEffectivePermissions(e.getClient().getSelfId().orElseThrow()).map(perms -> perms.contains(Permission.SEND_MESSAGES)))
                                                                                .next()
                                                                        )
                                                                        .flatMap(tc -> tc.createMessage(mcsc))
                                                                        .next()
                                                                        .flatMap(x -> c.createMessage("sent"))
                                                                        .flatMap(x -> msg.delete())
                                                                )
                                                                .subscribe();
                                                        d.get().dispose();
                                                    }else if(em.equals(BotUtils.x)){
                                                        c.createMessage("cancelled")
                                                                .flatMap(msg -> m.delete())
                                                                .subscribe();
                                                        d.get().dispose();
                                                    }
                                                })
                                                .subscribe()
                                        );
                                    })
                                    .flatMapMany(m -> Flux.fromIterable(Arrays.asList(BotUtils.checkmark, BotUtils.x))
                                            .flatMap(em -> m.addReaction(em))
                                    )
                                    .subscribe();
                            return true;
                        })
                )
                .requiresBotOwner(true)
                .register();
        new Command("sql", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> !args.isEmpty())
                .flatMap(c -> {
                    String sql = String.join(" ", args);
                    SQLExecutor executor = DataManager.executeSQL(sql);
                    if(!executor.isSuccessful())
                        return c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.sql.error", executor.getException().substring(0, Math.min(2048, executor.getException().length())))).map(x -> true);
                    if(executor.isQuery()){
                        SafeResultSet rs = executor.getResultSet();
                        ObjectMapper mapper = new ObjectMapper();
                        ArrayNode results = mapper.createArrayNode();
                        boolean includeTokens = sql.toLowerCase().contains("token");
                        while(rs.next()){
                            ObjectNode n = mapper.createObjectNode();
                            for(String colName : rs.getColumnNames()){
                                // remove all tokens due to safety reasons, except they are explicitly selected
                                if(colName.contains("token") && !includeTokens) continue;
                                n.set(colName, mapper.convertValue(rs.getObject(colName), JsonNode.class));
                            }
                            results.add(n);
                        }
                        try{
                            String result = mapper.writer(new CustomPrettyPrinter().withObjectFieldValueSeparator(": ")).writeValueAsString(results).replace("\r", "");
                            InputStream stream = IOUtils.toInputStream(result, StandardCharsets.UTF_8);
                            String r = Arrays.stream(result.split("\n")).map(String::trim).collect(Collectors.joining("\n"));
                            return c.createMessage(mcs -> mcs
                                    .setEmbed(LocaleManager.getLanguageMessage(lang, "commands.sql.result", r.substring(0, Math.min(2048-11, r.length()))))
                                    .addFile("result.json", stream)
                            ).map(x -> true);
                        }catch(Exception ex){
                            ex.printStackTrace();
                            return Mono.just(BotUtils.sendErrorMessage(c));
                        }
                    }else{
                        return c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.sql.success", ""+executor.getAffectedRowCount())).map(x -> true);
                    }
                }))
                .requiresBotOwner(true)
                .register();


        new Command("botsuggest", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> args.size() > 0)
                .map(c -> {
                    if((args.size() == 1 || args.size() == 2) && args.get(0).equalsIgnoreCase("list")){
                        AtomicLong pageNumber = new AtomicLong(1);
                        long itemsPerPage = 5;
                        if(args.size() == 2){
                            try{
                                pageNumber.set(Long.parseLong(args.get(1)));
                                if(pageNumber.get() < 2)
                                    pageNumber.set(1);
                            }catch(NumberFormatException ignored){
                            }
                        }
                        long maxPageNumber = DataManager.getBotSuggestionPageCount(itemsPerPage);
                        pageNumber.set(BotUtils.clamp(pageNumber.get(), 1L, maxPageNumber));
                        List<SQLBotSuggestion> suggestions = DataManager.getBotSuggestions(pageNumber.get(), itemsPerPage);
                        String s = suggestions.stream().map(suggestion -> suggestion.getStatus().getEmoji().asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw).orElseThrow() + " #" + suggestion.getId() + ": " + suggestion.getTitle()).collect(Collectors.joining("\n"));
                        c.createEmbed(ecs -> ecs
                                .setTitle("Bot Suggestions")
                                .setDescription(s)
                                .setFooter("Page " + pageNumber.get() + "/" + maxPageNumber, null)
                        ).flatMapMany(m -> Flux.fromIterable(Arrays.asList(BotUtils.arrowLeft, BotUtils.arrowRight))
                                .flatMap(em -> m.addReaction(em))
                        ).subscribe();
                        return true;
                    }else if(args.size() == 2 && (args.get(0).equalsIgnoreCase("upvote") || args.get(0).equalsIgnoreCase("downvote"))){
                        try{
                            boolean upvote = args.get(0).equalsIgnoreCase("upvote");
                            int sId = Integer.parseInt(args.get(1));
                            SQLBotSuggestion suggestion = DataManager.getBotSuggestion(sId);
                            if(suggestion == null){
                                c.createMessage("Could not find a suggestion with the id " + sId).subscribe();
                                return true;
                            }
                            if(DataManager.setBotSuggestionVote(e.getMessage().getAuthor().get().getId().asLong(), suggestion.getId(), upvote)){
                                c.createMessage("successfully voted for suggestion " + sId).subscribe();
                            }else{
                                BotUtils.sendErrorMessage(c);
                            }
                            return true;
                        }catch (NumberFormatException ex){
                            return false;
                        }
                    }else if(args.size() == 2 && args.get(0).equalsIgnoreCase("get")){
                        try{
                            int sId = Integer.parseInt(args.get(1));
                            SQLBotSuggestion suggestion = DataManager.getBotSuggestion(sId);
                            if(suggestion == null){
                                c.createMessage("Could not find a suggestion with the id " + sId).subscribe();
                                return true;
                            }
                            User u = e.getClient().getUserById(Snowflake.of(suggestion.getCreatorId())).block();
                            c.createEmbed(ecs -> ecs
                                    .setAuthor(u == null ? "user not found" : u.getUsername() + "#" + u.getDiscriminator(), null, u == null ? null : u.getAvatarUrl())
                                    .setTitle("Suggestion #" + sId + ": " + suggestion.getTitle())
                                    .setDescription(suggestion.getContent())
                                    .addField("Status: " + suggestion.getStatus().getName(), suggestion.getDetailedStatus().orElse("No description set"), false)
                                    .addField("Last update", BotUtils.getDuration(e.getMessage().getTimestamp().minusMillis(suggestion.getLastUpdate().toEpochMilli()).getEpochSecond()) + " ago", false)
                                    .setColor(suggestion.getStatus().getColor())
                                    .setFooter(suggestion.getUpvotes() + " upvotes, " + suggestion.getDownvotes() + " downvotes — created at", null)
                                    .setTimestamp(suggestion.getCreatedAt())
                            ).subscribe();
                        }catch(NumberFormatException ex){
                            return false;
                        }
                        return true;
                    }else if(args.size() == 2 && args.get(0).equalsIgnoreCase("notify")){
                        // set notification status for given suggestion
                        try{
                            int sId = Integer.parseInt(args.get(1));
                            SQLBotSuggestion suggestion = DataManager.getBotSuggestion(sId);
                            if(suggestion == null){
                                c.createMessage("could not find a suggestion with the id " + sId).subscribe();
                                return true;
                            }
                            boolean notif = DataManager.getBotSuggestionNotifications(sId).contains(e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow());
                            if(DataManager.setBotSuggestionNotification(e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow(), sId, !notif))
                                c.createMessage(notif ? "You no longer get notified on suggestion " + sId : "You now get notified on suggestion " + sId).subscribe();
                            else
                                return BotUtils.sendErrorMessage(c);
                        }catch(NumberFormatException ex){
                            c.createMessage("no valid id").subscribe();
                            return true;
                        }
                        return true;
                    }else if(args.size() > 2 && (args.get(0).equalsIgnoreCase("add") || args.get(0).equalsIgnoreCase("create") || args.get(0).equalsIgnoreCase("suggest"))){
                        String title = args.get(1);
                        String content = String.join(" ", args.subList(2, args.size()));
                        int suggestionId = DataManager.addBotSuggestion(e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow(), title, content, e.getMessage().getTimestamp());
                        if(suggestionId > -1){
                            DataManager.setBotSuggestionNotification(e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow(), suggestionId, true);
                            c.createMessage("created suggestion with id " + suggestionId).subscribe();
                            e.getClient().getChannelById(Snowflake.of(551801738223419392L)).ofType(GuildMessageChannel.class).flatMap(tc -> tc.createEmbed(ecs -> ecs
                                    .setTitle("New Suggestion #" + suggestionId)
                                    .addField(title, content, false)
                                    .setAuthor(e.getMessage().getAuthor().get().getUsername() + "#" + e.getMessage().getAuthor().get().getDiscriminator(), null, e.getMessage().getAuthor().get().getAvatarUrl())
                                    .setFooter("Created at", null)
                                    .setTimestamp(e.getMessage().getTimestamp())
                                    .setColor(BotUtils.botColor)
                            )).subscribe();
                        }else
                            return BotUtils.sendErrorMessage(c);
                        return true;
                    }else if(args.size() > 3 && args.get(0).equalsIgnoreCase("update")){
                        if(!BotUtils.getBotAdmins().contains(e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow()))
                            return BotUtils.sendNoPermissionsMessage(c, lang);
                        try{
                            int sId = Integer.parseInt(args.get(1));
                            SQLBotSuggestion suggestion = DataManager.getBotSuggestion(sId);
                            if(suggestion == null){
                                c.createMessage("could not find suggestion with id " + sId).subscribe();
                                return true;
                            }
                            if(args.get(2).equalsIgnoreCase("title")){
                                String newTitle = String.join(" ", args.subList(3, args.size()));
                                if(DataManager.setBotSuggestion(sId, "title", newTitle, JDBCType.VARCHAR))
                                    c.createMessage("changed title of suggestion #" + sId + " to " + newTitle).subscribe();
                                else
                                    return BotUtils.sendErrorMessage(c);
                                return true;
                            }else if(args.get(2).equalsIgnoreCase("content") || args.get(2).equalsIgnoreCase("description")){
                                String newContent = String.join(" ", args.subList(3, args.size()));
                                if(DataManager.setBotSuggestion(sId, "content", newContent, JDBCType.VARCHAR))
                                    c.createMessage("changed description of suggestion #" + sId + " to " + newContent).subscribe();
                                else
                                    return BotUtils.sendErrorMessage(c);
                                return true;
                            }else if(args.size() > 3 && args.get(2).equalsIgnoreCase("status")){
                                SuggestionStatus newStatus = SuggestionStatus.getSuggestionStatus(args.get(3));
                                if(newStatus == null) return false;
                                String newDetailedStatus = args.size() == 4 ? null : String.join(" ", args.subList(4, args.size()));
                                if(DataManager.setBotSuggestionStatus(sId, newStatus.getStatus(), newDetailedStatus, e.getMessage().getTimestamp())){
                                    c.createMessage("changed status of suggestion #" + sId + " to " + newStatus.getName() + ": " + newDetailedStatus).subscribe();
                                    Flux.fromIterable(DataManager.getBotSuggestionNotifications(sId))
                                            .flatMap(uId -> e.getClient().getUserById(Snowflake.of(uId)))
                                            .flatMap(User::getPrivateChannel)
                                            .flatMap(pc -> pc.createEmbed(ecs -> ecs
                                                    .setTitle("Updated suggestion #" + sId + ": " + suggestion.getTitle())
                                                    .setDescription("**New status**: " + newStatus.getName() + "\n\n" + newDetailedStatus)
                                                    .setFooter("Updated", null)
                                                    .setTimestamp(e.getMessage().getTimestamp())
                                                    .setColor(BotUtils.botColor)
                                            ))
                                            .subscribe();
                                }else
                                    return BotUtils.sendErrorMessage(c);
                                return true;
                            }
                        }catch(NumberFormatException ignored){
                        }
                        return false;
                    }
                    return false;
                }))
                .withAliases("botsuggestion", "botsuggestions")
                .usableByEveryone(true)
                .usableInDM(true)
                .register();


        new Command("feedback", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> args.size() > 0)
                .map(c -> {
                    if((args.size() == 1 || args.size() == 2) && args.get(0).equalsIgnoreCase("list")){
                        AtomicLong pageNumber = new AtomicLong(1);
                        long itemsPerPage = 5;
                        if(args.size() == 2){
                            try{
                                pageNumber.set(Long.parseLong(args.get(1)));
                                if(pageNumber.get() < 2)
                                    pageNumber.set(1);
                            }catch(NumberFormatException ignored){
                            }
                        }
                        long maxPageNumber = DataManager.getSuggestionPageCount(e.getGuildId().map(Snowflake::asLong).orElseThrow(), itemsPerPage);
                        pageNumber.set(BotUtils.clamp(pageNumber.get(), 1L, maxPageNumber));
                        List<SQLFeedback> suggestions = DataManager.getSuggestions(e.getGuildId().map(Snowflake::asLong).orElseThrow(), pageNumber.get(), itemsPerPage);
                        String s = suggestions.stream().map(suggestion -> suggestion.getStatus().getEmoji().asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw).orElseThrow() + " #" + suggestion.getId() + ": " + suggestion.getTitle()).collect(Collectors.joining("\n"));
                        c.createEmbed(ecs -> ecs
                                .setTitle("Feedback")
                                .setDescription(s)
                                .setFooter("Page " + pageNumber.get() + "/" + maxPageNumber, null)
                        ).flatMapMany(m -> Flux.fromIterable(Arrays.asList(BotUtils.arrowLeft, BotUtils.arrowRight))
                                .flatMap(em -> m.addReaction(em))
                        ).subscribe();
                        return true;
                    }else if(args.size() == 2 && (args.get(0).equalsIgnoreCase("upvote") || args.get(0).equalsIgnoreCase("downvote"))){
                        try{
                            boolean upvote = args.get(0).equalsIgnoreCase("upvote");
                            int sId = Integer.parseInt(args.get(1));
                            SQLFeedback suggestion = DataManager.getSuggestion(e.getGuildId().get().asLong(), sId);
                            if(suggestion == null){
                                c.createMessage("Could not find a suggestion with the id " + sId).subscribe();
                                return true;
                            }
                            if(DataManager.setSuggestionVote(e.getGuildId().get().asLong(), e.getMessage().getAuthor().get().getId().asLong(), suggestion.getId(), upvote)){
                                c.createMessage("successfully voted for suggestion " + sId).subscribe();
                            }else{
                                BotUtils.sendErrorMessage(c);
                            }
                            return true;
                        }catch (NumberFormatException ex){
                            return false;
                        }
                    }else if(args.size() == 2 && args.get(0).equalsIgnoreCase("get")){
                        try{
                            int sId = Integer.parseInt(args.get(1));
                            SQLFeedback suggestion = DataManager.getSuggestion(e.getGuildId().map(Snowflake::asLong).orElseThrow(), sId);
                            if(suggestion == null){
                                c.createMessage("Could not find feedback with the id " + sId).subscribe();
                                return true;
                            }
                            User u = e.getClient().getUserById(Snowflake.of(suggestion.getCreatorId())).block();
                            c.createEmbed(ecs -> ecs
                                    .setAuthor(u == null ? "user not found" : u.getUsername() + "#" + u.getDiscriminator(), null, u == null ? null : u.getAvatarUrl())
                                    .setTitle("Feedback #" + sId + ": " + suggestion.getTitle())
                                    .setDescription(suggestion.getContent())
                                    .addField("Type", suggestion.getType().getName(), false)
                                    .addField("Status: " + suggestion.getStatus().getName(), suggestion.getDetailedStatus().orElse("No description set"), false)
                                    .addField("Last update", BotUtils.getDuration(e.getMessage().getTimestamp().minusMillis(suggestion.getLastUpdate().toEpochMilli()).getEpochSecond()) + " ago", false)
                                    .setColor(suggestion.getStatus().getColor())
                                    .setFooter(suggestion.getUpvotes() + " upvotes, " + suggestion.getDownvotes() + " downvotes — created at", null)
                                    .setTimestamp(suggestion.getCreatedAt())
                            ).subscribe();
                        }catch(NumberFormatException ex){
                            return false;
                        }
                        return true;
                    }else if(args.size() == 2 && args.get(0).equalsIgnoreCase("notify")){
                        // set notification status for given suggestion
                        try{
                            int sId = Integer.parseInt(args.get(1));
                            SQLFeedback suggestion = DataManager.getSuggestion(e.getGuildId().map(Snowflake::asLong).orElseThrow(), sId);
                            if(suggestion == null){
                                c.createMessage("could not find a suggestion with the id " + sId).subscribe();
                                return true;
                            }
                            boolean notif = DataManager.getSuggestionNotifications(e.getGuildId().map(Snowflake::asLong).orElseThrow(), sId).contains(e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow());
                            if(DataManager.setSuggestionNotification(e.getGuildId().map(Snowflake::asLong).orElseThrow(), e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow(), sId, !notif))
                                c.createMessage(notif ? "You no longer get notified on suggestion " + sId : "You now get notified on suggestion " + sId).subscribe();
                            else
                                return BotUtils.sendErrorMessage(c);
                        }catch(NumberFormatException ex){
                            c.createMessage("no valid id").subscribe();
                            return true;
                        }
                        return true;
                    }else if(args.size() > 3 && (args.get(0).equalsIgnoreCase("add") || args.get(0).equalsIgnoreCase("create") || args.get(0).equalsIgnoreCase("suggest"))){
                        if(RatelimitUtils.isMemberRateLimited(e.getGuildId().map(Snowflake::asLong).orElseThrow(), e.getMember().map(Member::getId).map(Snowflake::asLong).orElseThrow(), RatelimitUtils.RatelimitChannel.FEEDBACK, 2, 300_000, c, lang))
                            return true;
                        SQLFeedback.FeedbackType type = SQLFeedback.FeedbackType.getFeedbackType(args.get(1));
                        if(type == null) return false;
                        String title = args.get(2);
                        String content = String.join(" ", args.subList(3, args.size()));
                        SQLFeedback suggestion = DataManager.addSuggestion(e.getGuildId().map(Snowflake::asLong).orElseThrow(), e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow(), title, content, e.getMessage().getTimestamp(), type);
                        if(suggestion != null){
                            DataManager.setSuggestionNotification(e.getGuildId().map(Snowflake::asLong).orElseThrow(), e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow(), suggestion.getId(), true);
                            c.createMessage("created suggestion with id " + suggestion.getId()).subscribe();
                            e.getClient().getChannelById(Snowflake.of(DataManager.getGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow()).getSuggestionChannelId())).ofType(GuildMessageChannel.class).flatMap(tc -> tc.createEmbed(ecs -> ecs
                                    .setTitle("New Suggestion #" + suggestion.getId())
                                    .addField(title, content, false)
                                    .setAuthor(e.getMessage().getAuthor().get().getUsername() + "#" + e.getMessage().getAuthor().get().getDiscriminator(), null, e.getMessage().getAuthor().get().getAvatarUrl())
                                    .setFooter("Created at", null)
                                    .setTimestamp(e.getMessage().getTimestamp())
                                    .setColor(BotUtils.botColor)
                            )).subscribe();
                        }else
                            return BotUtils.sendErrorMessage(c);
                        return true;
                    }else if(args.size() > 3 && args.get(0).equalsIgnoreCase("update")){
                        if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "updateFeedback", false, Permission.MANAGE_GUILD))
                            return BotUtils.sendNoPermissionsMessage(c, lang);
                        try{
                            int sId = Integer.parseInt(args.get(1));
                            SQLFeedback suggestion = DataManager.getSuggestion(e.getGuildId().map(Snowflake::asLong).orElseThrow(), sId);
                            if(suggestion == null){
                                c.createMessage("could not find suggestion with id " + sId).subscribe();
                                return true;
                            }
                            if(args.get(2).equalsIgnoreCase("title")){
                                String newTitle = String.join(" ", args.subList(3, args.size()));
                                if(DataManager.setSuggestion(e.getGuildId().map(Snowflake::asLong).orElseThrow(), sId, "title", newTitle, JDBCType.VARCHAR))
                                    c.createMessage("changed title of suggestion #" + sId + " to " + newTitle).subscribe();
                                else
                                    return BotUtils.sendErrorMessage(c);
                                return true;
                            }else if(args.get(2).equalsIgnoreCase("content") || args.get(2).equalsIgnoreCase("description")){
                                String newContent = String.join(" ", args.subList(3, args.size()));
                                if(DataManager.setSuggestion(e.getGuildId().map(Snowflake::asLong).orElseThrow(), sId, "content", newContent, JDBCType.VARCHAR))
                                    c.createMessage("changed description of suggestion #" + sId + " to " + newContent).subscribe();
                                else
                                    return BotUtils.sendErrorMessage(c);
                                return true;
                            }else if(args.size() > 3 && args.get(2).equalsIgnoreCase("status")){
                                SuggestionStatus newStatus = SuggestionStatus.getSuggestionStatus(args.get(3));
                                if(newStatus == null) return false;
                                String newDetailedStatus = args.size() == 4 ? null : String.join(" ", args.subList(4, args.size()));
                                if(DataManager.setSuggestionStatus(e.getGuildId().map(Snowflake::asLong).orElseThrow(), sId, newStatus.getStatus(), newDetailedStatus, e.getMessage().getTimestamp())){
                                    c.createMessage("changed status of suggestion #" + sId + " to " + newStatus.getName() + ": " + newDetailedStatus).subscribe();
                                    Flux.fromIterable(DataManager.getSuggestionNotifications(e.getGuildId().map(Snowflake::asLong).orElseThrow(), sId))
                                            .flatMap(uId -> e.getClient().getUserById(Snowflake.of(uId)))
                                            .flatMap(User::getPrivateChannel)
                                            .flatMap(pc -> pc.createEmbed(ecs -> ecs
                                                    .setTitle("Updated suggestion #" + sId + ": " + suggestion.getTitle())
                                                    .setDescription("**New status**: " + newStatus.getName() + "\n\n" + newDetailedStatus)
                                                    .setFooter("Updated", null)
                                                    .setTimestamp(e.getMessage().getTimestamp())
                                                    .setColor(BotUtils.botColor)
                                            ))
                                            .subscribe();
                                }else
                                    return BotUtils.sendErrorMessage(c);
                                return true;
                            }
                        }catch(NumberFormatException ignored){
                        }
                        return false;
                    }
                    return false;
                }))
                .withPermissions("createFeedback")
                .usableByEveryone(true)
                .register();

        /* NSFW */

        new Command("boobs", (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(MessageChannel.class)
                .flatMap(c -> c.createMessage(NSFWUtils.getBoobs()))
                .thenReturn(true)
                )
                .nsfwOnly(true)
                .usableInDM(true)
                .register();
        new Command("ass", (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(MessageChannel.class)
                .flatMap(c -> c.createMessage(NSFWUtils.getAss()))
                .thenReturn(true)
                )
                .nsfwOnly(true)
                .usableInDM(true)
                .register();
        new Command("asian", (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(MessageChannel.class)
                .flatMap(c -> c.createMessage(NSFWUtils.getAsian()))
                .thenReturn(true)
                )
                .nsfwOnly(true)
                .usableInDM(true)
                .register();
        new Command("pussy", (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(MessageChannel.class)
                .flatMap(c -> c.createMessage(SFWUtils.getCat()))
                .thenReturn(true)
                )
                .nsfwOnly(true)
                .usableInDM(true)
                .register();
        new Command("cock", (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(MessageChannel.class)
                .flatMap(c -> c.createMessage(SFWUtils.getCock()))
                .thenReturn(true)
                )
                .nsfwOnly(true)
                .usableInDM(true)
                .register();

        new Command("backup", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> args.size() > 0)
                .map(c -> {
                    if(args.size() == 2 && (args.get(0).equalsIgnoreCase("automated") || args.get(0).equalsIgnoreCase("automation") || args.get(0).equalsIgnoreCase("automate") || args.get(0).equalsIgnoreCase("automatic"))){
                        if(PatreonManager.isPatronGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow())){
                            if(args.get(1).equalsIgnoreCase("enable")){
                                if(autoBackupGuilds.contains(e.getGuildId().map(Snowflake::asLong).orElseThrow())){
                                    // already enabled
                                }else{
                                    autoBackupGuilds.add(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                                    // added
                                }
                            }else if(args.get(1).equalsIgnoreCase("disable")){
                                if(autoBackupGuilds.remove(e.getGuildId().map(Snowflake::asLong).orElseThrow())){
                                    // removed
                                }else{
                                    // was not enabled
                                }
                            }else if(args.get(1).equalsIgnoreCase("get")){
                                // tell whether is is enabled or not
                                boolean enabled = autoBackupGuilds.contains(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            }
                        }else{
                            // can't use automated backups
                        }
                    }
                    if(args.size() > 1){
                        if(args.get(0).equalsIgnoreCase("create")){
                            String bId = String.join(" ", args.subList(1, args.size()));
                            if(DataManager.guildBackupExists(e.getGuildId().map(Snowflake::asLong).orElseThrow(), bId)){
                                // already exists
                                return true;
                            }
                            boolean isPatronGuild = PatreonManager.isPatronGuild(e.getGuildId().map(Snowflake::asLong).orElseThrow());
                            int backupCount = DataManager.getGuildBackupCount(e.getGuildId().map(Snowflake::asLong).orElseThrow(), false);
                            if(backupCount > 2 && !isPatronGuild){
                                // max 3 non patreon backups
                                return true;
                            }else if(backupCount > 14){
                                // max 15 patreon backups
                                return true;
                            }
                            if(!DataManager.createGuildBackup(e.getGuild().block(), args.get(1), false))
                                return BotUtils.sendErrorMessage(c);
                            // backup created
                            return true;
                        }else if(args.get(0).equalsIgnoreCase("restore")){
                            DataManager.restoreGuildBackup(e.getGuild().block(), args.get(1));
                        }else if(args.get(0).equalsIgnoreCase("info") || args.get(0).equalsIgnoreCase("information")){

                        }
                    }
                    return false;
                }))
                .withAliases("backups")
                .withPermissions("manageBackups", Permission.ADMINISTRATOR)
                .register();

        new Command("urbandictionary", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> !args.isEmpty())
                .map(c -> {
                    UrbanDictionary dictionary = new UrbanDictionary(String.join(" ", args));
                    UrbanDictionary.UrbanDefinition definition = dictionary.getVoteBasedDefinition();
                    if(definition == null){
                        c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.urbandictionary.noDefinitionFound", String.join(" ", args))).subscribe();
                        return true;
                    }
                    c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.urbandictionary.definition",
                            definition.getWord(),
                            definition.getFormattedDefinition(),
                            definition.getAuthorName(),
                            definition.getAuthorUrl(),
                            "" + definition.getUpvotes(),
                            "" + definition.getDownvotes(),
                            definition.getFormattedExample()
                    ).andThen(ecs -> ecs
                            .setUrl(definition.getUrl())
                            .setTimestamp(definition.getTime())
                    )).subscribe();
                    return true;
                }))
                .withAliases("define")
                .withPermissions("urbandictionary")
                .usableByEveryone(true)
                .usableInDM(true)
                .nsfwOnly(true)
                .register();

        new Command("wikipedia", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> !args.isEmpty())
                .doOnNext(c -> {
                    String word = String.join(" ", args);
                    Wikipedia article = new Wikipedia(lang, word);
                    if(!article.hasFound()){
                        c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.wikipedia.noArticleFound", word)).subscribe();
                    }else{
                        String extract = article.getExtract().length() > 2048 ? article.getExtract().substring(0, 2045).strip() + "..." : article.getExtract();
                        c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.wikipedia.extract", article.getTitle(), extract).andThen(ecs -> ecs.setUrl(article.getUrl()))).subscribe();
                    }
                })
                .thenReturn(true)
                )
                .withAliases("wiki")
                .withPermissions("wikipedia")
                .usableByEveryone(true)
                .usableInDM(true)
                .nsfwOnly(true)
                .register();

        new Command("clear", (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(GuildMessageChannel.class)
                .filter(c -> args.size() == 1 || args.size() == 2)
                .flatMap(c -> {
                    try{
                        int amount = Integer.parseInt(args.get(0));
                        if(amount < 2 || amount > 100)
                            return c.createEmbed(LocaleManager.getLanguageMessage(lang, "commands.clear.invalidAmount")).map(m -> true);
                        if(args.size() == 2){
                            Instant ago = Instant.now().minus(14, ChronoUnit.DAYS);
                            Mono<User> user = BotUtils.getUserFromArgument(args.get(1));
                            Flux<Snowflake> messages = user
                                    .flatMapMany(u -> c.getMessagesBefore(e.getMessage().getId())
                                            .takeWhile(m -> m.getId().getTimestamp().isAfter(ago))
                                            .filter(m -> m.getAuthor().map(User::getId).map(u.getId()::equals).orElse(false))
                                            .take(amount)
                                            .map(Message::getId)
                                    );
                            return user
                                    .flatMap(u -> c.bulkDelete(messages).count()
                                            .flatMap(cnt -> messages.count()
                                                    .flatMap(mCnt -> c.createMessage("deleted " + (mCnt - cnt) + " of " + mCnt + " found messages"))
                                            )
                                    )
                                    .switchIfEmpty(c.createMessage("could not find user"))
                                    .map(x -> true);
                        }else{
                            Instant ago = Instant.now().minus(14, ChronoUnit.DAYS);
                            Flux<Snowflake> messages = c.getMessagesBefore(e.getMessage().getId())
                                    .takeWhile(m -> m.getId().getTimestamp().isAfter(ago))
                                    .take(amount)
                                    .map(Message::getId);
                            return c.bulkDelete(messages).count()
                                    .flatMap(cnt -> messages.count()
                                            .flatMap(mCnt -> c.createMessage("deleted " + (mCnt - cnt) + " of " + mCnt + " found messages"))
                                    )
                                    .map(x -> true);
                        }
                    }catch(NumberFormatException ex){
                        return Mono.just(false);
                    }
                }))
                .withPermissions("clear", Permission.MANAGE_MESSAGES)
                .withRatelimit(new RatelimitUtils.Ratelimit(2, 10_000))
                .register();

        new Command("clear", (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(GuildMessageChannel.class)
                .filter(c -> args.size() == 1 || args.size() == 2)
                .flatMap(c -> {
                    try{
                        if(args.size() == 1){
                            int amount = Integer.parseInt(args.get(0));
                            if(amount < 2 || amount > 100)
                                return c.createMessage("must be between 2 and 100").map(x -> true);
                            else{
                                Flux<Snowflake> messages = c.getMessagesBefore(e.getMessage().getId())
                                        .take(amount)
                                        .map(Message::getId);
                                return c.bulkDelete(messages).count().flatMap(cnt -> messages.count()
                                        .flatMap(cnt2 -> c.createMessage("deleted " + (cnt2 - cnt) + " of " + cnt2 + " messages"))
                                ).map(x -> true);
                            }
                        }else{
                            long from = Long.parseLong(args.get(0));
                            long to = Long.parseLong(args.get(1));
                            Snowflake start = Snowflake.of(BotUtils.isSnowflake(to) ? Math.min(from, to)-1 : from-1);
                            Snowflake end = Snowflake.of(BotUtils.isSnowflake(from) ? Math.max(from,to)+1 : to+1);
                            if(BotUtils.isSnowflake(from)){
                                if(start.getTimestamp().isBefore(Instant.now().minus(14, ChronoUnit.DAYS)))
                                    return c.createMessage("the start message is older than two weeks. please select a newer message.").map(x -> true);
                                if(BotUtils.isSnowflake(to)){
                                    Flux<Snowflake> messages = c.getMessagesAfter(start)
                                            .takeWhile(message -> message.getId().asLong() < end.asLong())
                                            .take(100)
                                            .map(Message::getId);
                                    return c.bulkDelete(messages).count().flatMap(cnt -> messages.count()
                                            .flatMap(cnt2 -> c.createMessage("deleted " + (cnt2 - cnt) + " of " + cnt2 + " messages"))
                                    ).map(x -> true);
                                }else{
                                    if(to < 2 || to > 100)
                                        return c.createMessage("must be between 2 and 100").map(x -> true);
                                    else{
                                        Flux<Snowflake> messages = c.getMessagesAfter(start)
                                                .take(to)
                                                .map(Message::getId);
                                        return c.bulkDelete(messages).count().flatMap(cnt -> messages.count()
                                                .flatMap(cnt2 -> c.createMessage("deleted " + (cnt2 - cnt) + " of " + cnt2 + " messages"))
                                        ).map(x -> true);
                                    }
                                }
                            }else if(BotUtils.isSnowflake(to)){
                                if(end.getTimestamp().isBefore(Instant.now().minus(14, ChronoUnit.DAYS)))
                                    return c.createMessage("the start message is older than two weeks. please select a newer message.").map(x -> true);
                                Flux<Snowflake> messages = c.getMessagesBefore(end)
                                        .takeWhile(message -> message.getId().getTimestamp().isAfter(Instant.now().minus(14, ChronoUnit.DAYS)))
                                        .take(from)
                                        .map(Message::getId);
                                return c.bulkDelete(messages).count().flatMap(cnt -> messages.count()
                                        .flatMap(cnt2 -> c.createMessage("deleted " + (cnt2 - cnt) + " of " + cnt2 + " messages"))
                                ).map(x -> true);
                            }
                            return Mono.just(false);
                        }
                    }catch(NumberFormatException ex){
                        return Mono.just(false);
                    }
                }))
                .register();

        new Command("antiraid", (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> args.size() == 1 || args.size() == 2)
                .map(c -> {
                    try{
                        Snowflake start = Snowflake.of(args.get(0));
                        long to = args.size() == 2 ? Long.parseLong(args.get(1)) : 5;
                        Snowflake end = to < start.asLong() ? Snowflake.of(start.getTimestamp().plus(to, ChronoUnit.MINUTES)) : Snowflake.of(to);
                        if(start.getTimestamp().isBefore(Instant.now().minus(5, ChronoUnit.DAYS))){
                            c.createMessage("You are trying to delete a raid that is older than 5 days. To prevent accidents this is not possible.").subscribe();
                            return true;
                        }
                        e.getGuild().flatMapMany(Guild::getMembers)
                                // 3 minutes buffer
                                .filter(m -> m.getJoinTime().isAfter(start.getTimestamp().minusSeconds(180)) && m.getJoinTime().isBefore(end.getTimestamp().plusSeconds(1)))
                                .flatMap(m -> m.ban(bqs -> bqs
                                        .setReason("Raid clear by " + e.getMember().map(Member::getId).map(Snowflake::asString).orElseThrow())
                                        .setDeleteMessageDays(7)
                                ).thenReturn(true))
                                .count()
                                .flatMap(cnt -> c.createMessage("Banned " + cnt + " users"))
                                .subscribe();
                        return true;
                    }catch(NumberFormatException ex){
                        return false;
                    }
                }))
                .register();
    }

}