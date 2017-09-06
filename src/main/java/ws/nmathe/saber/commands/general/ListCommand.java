package ws.nmathe.saber.commands.general;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ws.nmathe.saber.Main;
import ws.nmathe.saber.commands.Command;
import ws.nmathe.saber.core.schedule.ScheduleEntry;
import ws.nmathe.saber.utils.Logging;
import ws.nmathe.saber.utils.MessageUtilities;
import ws.nmathe.saber.utils.VerifyUtilities;

import java.util.*;
import java.util.stream.Collectors;

/**
 */
public class ListCommand implements Command
{
    @Override
    public String name()
    {
        return "list";
    }

    @Override
    public String help(String prefix, boolean brief)
    {
        String head = prefix + this.name();

        String USAGE_EXTENDED = "```diff\n- Usage\n" + head + " <ID> [mode] [filters]```\n" +
                "The list command will show all users who have rsvp'ed yes.\n" +
                "The command takes a single argument which should be the ID of the event you wish query.\n" +
                "\nThe schedule holding the event must have 'rsvp' turned on in the configuration settings.\n" +
                "RSVP can be enabled on a channel using the config command as followed, ``" +
                prefix + "config #channel rsvp on``\n" +
                "\nThe list may be filtered by either users or roles by appending \"r: @role\", \"u: @user\", or " +
                "\"t: [type]\" to the command.\n" +
                "Any number of filters may be appended to the command.\n" +
                "To display only users who have not rsvp'ed, use *no-input* as the ``[type]``.\n\n" +
                "The list command has two optional 'modes' of display. \n" +
                "If the term 'mobile' is added as an argument to the command, non-mentionable usernames will be displayed.\n" +
                "If the term 'id' is added as an argument, usernames will be displayed as they escaped mentionable ID tags.";

        String USAGE_BRIEF = "``" + head + "`` - show an event's rsvp list";

        String USAGE_EXAMPLES = "```diff\n- Examples```\n" +
                "``" + head + " 080194c``\n" +
                "``" + head + " 01d9aff \"u: @notem\"\n" +
                "``" + head + " 0a9dda2 mobile \"t: yes\" \"t: no\" \"t: undecided\"";

        if( brief ) return USAGE_BRIEF;
        else return USAGE_BRIEF + "\n\n" + USAGE_EXTENDED + "\n\n" + USAGE_EXAMPLES;
    }

    @Override
    public String verify(String prefix, String[] args, MessageReceivedEvent event)
    {
        String head = prefix + this.name();

        if (args.length==0)
        {
            return "That's not enough arguments! Use ``" + head + " <ID> [filters]``";
        }

        int index = 0;

        ScheduleEntry entry;
        if (VerifyUtilities.verifyHex(args[index]))
        {
            Integer entryId = Integer.decode("0x" + args[index]);
            entry = Main.getEntryManager().getEntryFromGuild(entryId, event.getGuild().getId());
            if (entry == null)
            {
                return "The requested entry does not exist!";
            }
            if (!Main.getScheduleManager().isRSVPEnabled(entry.getScheduleID()))
            {
                return "The schedule that the entry is on is not rsvp enabled!";
            }
        }
        else
        {
            return "Argument *" + args[index] + "* is not a valid entry ID!";
        }

        index++;

        boolean mobileFlag = false;
        boolean IdFlag = false;
        for(; index<args.length; index++)
        {
            if(args[index].equalsIgnoreCase("mobile") || args[index].equalsIgnoreCase("m"))
            {
                mobileFlag = true;
                if(IdFlag)
                {
                    return "Both 'mobile' and 'id' modes cannot be active at the same time!\n" +
                            "Include either the 'id' or 'mobile' argument in the command, not both.";
                }
                continue;
            }
            if(args[index].equalsIgnoreCase("id") || args[index].equalsIgnoreCase("i"))
            {
                IdFlag = true;
                if(mobileFlag)
                {
                    return "Both 'mobile' and 'id' modes cannot be active at the same time!\n" +
                            "Include either the 'id' or 'mobile' argument in the command, not both.";
                }
                continue;
            }

            String[] filter = args[index].toLowerCase().split(":");
            if(filter.length != 2)
            {
                return "Invalid filter ``" + args[index] + "``!\nFilters must be of the form \"r: @role\" or \"u: @user\"";
            }

            String filterType = filter[0].trim();
            String filterValue = filter[1].trim();
            switch(filterType)
            {
                case "r":
                case "role":
                    break;

                case "u":
                case "user":
                    break;

                case "t":
                case "type":
                    if(filterValue.equalsIgnoreCase("no-input")) break;
                    Map<String, String> options = Main.getScheduleManager().getRSVPOptions(entry.getChannelId());
                    if(!options.values().contains(filterValue))
                    {
                        return "Invalid ``[type]`` for type filter!" +
                                "``[type]`` must be either an rsvp group used on that event or \"no-input\".";
                    }
                    break;

                default:
                    return "Invalid filter ``" + args[index] + "``!" +
                            "\nFilters must be of the form \"r: @role\", \"u: @user\", or \"t: [type]\"";
            }
        }

        return "";
    }

    @Override
    public void action(String prefix, String[] args, MessageReceivedEvent event)
    {
        try
        {
            int index = 0;
            Integer entryId = Integer.decode("0x" + args[index++]);
            ScheduleEntry se = Main.getEntryManager().getEntryFromGuild(entryId, event.getGuild().getId());

            String content = "";

            List<String> userFilters = new ArrayList<>();
            List<String> roleFilters = new ArrayList<>();
            boolean filterByType = false;
            Set<String> typeFilters = new HashSet<>();

            boolean mobileFlag = false;
            boolean IdFlag = false;
            for(; index<args.length; index++)
            {
                if(args[index].equalsIgnoreCase("mobile") || args[index].equalsIgnoreCase("m"))
                {
                    mobileFlag = true;
                    continue;
                }
                if(args[index].equalsIgnoreCase("id") || args[index].equalsIgnoreCase("i"))
                {
                    IdFlag = true;
                    continue;
                }

                String filterType = args[index].toLowerCase().split(":")[0].trim();
                String filterValue = args[index].toLowerCase().split(":")[1].trim();
                switch(filterType)
                {
                    case "r":
                    case "role":
                        roleFilters.add(filterValue.replace("<@&","").replace(">",""));
                        break;

                    case "u":
                    case "user":
                        userFilters.add(filterValue.replace("<@","").replace(">",""));
                        break;

                    case "t":
                    case "type":
                        filterByType = true;
                        typeFilters.add(filterValue);
                        break;
                }
            }

            Map<String, String> options = Main.getScheduleManager().getRSVPOptions(se.getChannelId());
            for(String type : options.values())
            {
                if(!filterByType || typeFilters.contains(type))
                {
                    content += "**\"" + type + "\"\n======================**\n";
                    List<String> members = se.getRsvpMembersOfType(type);
                    for(String id : members)
                    {
                        if(content.length() > 1900)
                        {
                            MessageUtilities.sendMsg(content, event.getChannel(), null);
                            content = "*continued. . .* \n";
                        }
                        Member member = event.getGuild().getMemberById(id);
                        if(this.checkMember(member, userFilters, roleFilters))
                        {
                            if(mobileFlag)
                            {
                                content += event.getGuild().getMemberById(id).getEffectiveName() + "\n";
                            }
                            else if(IdFlag)
                            {
                                content += " \\<@" + id + ">\n";
                            }
                            else
                            {
                                content += " <@" + id + ">\n";
                            }
                        }
                    }
                }
            }

            if(!filterByType || typeFilters.contains("no-input"))
            {
                List<String> undecided = event.getGuild().getMembers().stream()
                        .filter(member -> checkMember(member, userFilters, roleFilters))
                        .map(member -> member.getUser().getId()).collect(Collectors.toList());

                for(String type : options.values())
                {
                    undecided.removeAll(se.getRsvpMembersOfType(type));
                }

                content += "**\nNo input\n======================\n**";
                if(!filterByType & undecided.size() > 10)
                {
                    content += " Too many users to show: " + undecided.size() + " users with no rsvp\n";
                }
                else for(String id : undecided)
                {
                    if(content.length() > 1900)
                    {
                        MessageUtilities.sendMsg((new MessageBuilder()).setEmbed(
                                (new EmbedBuilder()).setDescription(content).build()
                        ).build(), event.getChannel(), null);
                        content = "*continued. . .* \n";
                    }
                    if(mobileFlag)
                    {
                        content += event.getGuild().getMemberById(id).getEffectiveName() + "\n";
                    }
                    else if(IdFlag)
                    {
                        content += " \\<@" + id + ">\n";
                    }
                    else
                    {
                        content += " <@" + id + ">\n";
                    }
                }
            }

            // build and send the embedded message object
            String titleUrl = se.getTitleUrl()==null ? "https://nnmathe.ws/saber": se.getTitleUrl();
            String title = se.getTitle()+" ["+Integer.toHexString(entryId)+"]";
            MessageUtilities.sendMsg((new MessageBuilder()).setEmbed(
                    (new EmbedBuilder())
                            .setDescription(content)
                            .setTitle(title, titleUrl)
                            .build()
            ).build(), event.getChannel(), null);
        }
        catch(Exception e)
        {
            Logging.exception(this.getClass(), e);
        }
    }

    private boolean checkMember(Member member, List<String> userFilters, List<String> roleFilters)
    {
        if(member!=null)
        {
            boolean skip = false;
            if(!userFilters.isEmpty() && !userFilters.contains(member.getUser().getId()))
            {
                skip = true;
            }
            else if(!roleFilters.isEmpty())
            {
                skip = true;
                for(String role : roleFilters)
                {
                    List<String> memberRoles = member.getRoles().stream()
                            .map(ISnowflake::getId).collect(Collectors.toList());
                    if(memberRoles.contains(role))
                    {
                        skip = false;
                        break;
                    }
                }
            }
            return !skip;
        }
        return false;
    }
}
