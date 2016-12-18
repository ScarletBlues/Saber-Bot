package io.schedulerbot.commands.general;

import io.schedulerbot.Main;
import io.schedulerbot.commands.Command;
import io.schedulerbot.core.ScheduleEntry;
import io.schedulerbot.core.ScheduleManager;
import io.schedulerbot.utils.MessageUtilities;
import io.schedulerbot.utils.VerifyUtilities;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 */
public class DestroyCommand implements Command
{
    private static String prefix = Main.getSettings().getCommandPrefix();
    private static String scheduleChan = Main.getSettings().getScheduleChan();

    private static final String USAGE_EXTENDED = "\nCalling **!destroy <ID>** will end the event with <ID>" +
            " prematurely. If **all** is used instead of the event ID, all scheduled events will be destroyed." +
            "\nEx1: **!destroy 084c**\nEx2: **!destroy all**";

    private static final String USAGE_BRIEF = "**" + prefix + "destroy** - Removes an entry from " +
            scheduleChan + ", sending an event ended early or canceled announcement.";

    private ScheduleManager scheduleManager = Main.scheduleManager;

    @Override
    public String help(boolean brief)
    {
        if( brief )
            return USAGE_BRIEF;
        else
            return USAGE_BRIEF + "\n" + USAGE_EXTENDED;
    }

    @Override
    public boolean verify(String[] args, MessageReceivedEvent event)
    {
        if(args.length>1 || args.length==0)
            return false;
        if( args[0].equals("all") )
        {
            return true;
        }
        if( !VerifyUtilities.verifyHex( args[0] ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event)
    {
        Guild guild = event.getGuild();

        if( args[0].equals("all") )
        {
            ArrayList<Integer> ent = scheduleManager.getEntriesByGuild( guild.getId() );
            if( ent == null )
            {
                MessageUtilities.sendMsg(
                        "Your guild has no entries on the schedule.",
                        event.getChannel(), null);
                return;
            }
            ArrayList<Integer> entries = new ArrayList<>();
            entries.addAll( ent );
            for (Integer eId : entries)
            {
                ScheduleEntry entry = scheduleManager.getEntry(eId);
                if (entry != null)
                {
                    // create the announcement message strings
                    String cancelMsg = "@everyone The event **" + entry.eTitle
                            + "** has been cancelled.";
                    String earlyMsg = "@everyone The event **" + entry.eTitle
                            + "** has ended early.";

                    // compare the current time to the start time
                    long dif = entry.eStart.until(ZonedDateTime.now(), SECONDS);

                    // if the difference is less than 0 the event was ended early
                    if (dif < 24*60*60)
                        MessageUtilities.sendAnnounce(earlyMsg, guild, null);

                        // otherwise event was canceled before it began
                    else
                        MessageUtilities.sendAnnounce(cancelMsg, guild, null);

                    synchronized( scheduleManager.getScheduleLock() )
                    {
                        scheduleManager.removeEntry(eId);
                    }

                    // delete the old message
                    MessageUtilities.deleteMsg(entry.eMsg, null);
                }
            }
        }

        else
        {
            Integer entryId = Integer.decode("0x" + args[0]);
            ScheduleEntry entry = scheduleManager.getEntry(entryId);

            if (entry == null)
            {
                String msg = "There is no event entry with ID " + args[0] + ".\"";
                MessageUtilities.sendMsg(msg, event.getChannel(), null);
                return;
            }

            // create the announcement message strings
            String cancelMsg = "@everyone The event **" + entry.eTitle
                    + "** has been cancelled.";
            String earlyMsg = "@everyone The event **" + entry.eTitle
                    + "** has ended early.";

            // compare the current time to the start time
            long dif = entry.eStart.until(ZonedDateTime.now(), SECONDS);

            // if the difference is less than 0 the event was ended early
            if (dif < 24*60*60)
                MessageUtilities.sendAnnounce(earlyMsg, guild, null);

                // otherwise event was canceled before it began
            else
                MessageUtilities.sendAnnounce(cancelMsg, guild, null);

            synchronized( scheduleManager.getScheduleLock() )
            {
                scheduleManager.removeEntry(entryId);
            }

            // delete the old entry
            MessageUtilities.deleteMsg(entry.eMsg, null);

        }
    }
}
