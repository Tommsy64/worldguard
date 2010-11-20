// $Id$
/*
 * WorldGuard
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 *
 * @author sk89q
 */
public class BlacklistEntry {
    /**
     * Used to prevent spamming.
     */
    private static Map<String,BlacklistTrackedEvent> lastAffected =
            new HashMap<String,BlacklistTrackedEvent>();
    /**
     * Parent blacklist entry.
     */
    private Blacklist blacklist;
    /**
     * List of groups to not affect.
     */
    private Set<String> ignoreGroups;
    /**
     * List of actions to perform on destruction.
     */
    private String[] destroyActions;
    /**
     * List of actions to perform on left click.
     */
    private String[] destroyWithActions;
    /**
     * List of actions to perform on right click.
     */
    private String[] createActions;
    /**
     * List of actions to perform on right click upon.
     */
    private String[] useActions;
    /**
     * List of actions to perform on drop.
     */
    private String[] dropActions;

    /**
     * Construct the object.
     * 
     * @param blacklist
     */
    public BlacklistEntry(Blacklist blacklist) {
        this.blacklist = blacklist;
    }

    /**
     * @return the ignoreGroups
     */
    public String[] getIgnoreGroups() {
        return ignoreGroups.toArray(new String[ignoreGroups.size()]);
    }

    /**
     * @param ignoreGroups the ignoreGroups to set
     */
    public void setIgnoreGroups(String[] ignoreGroups) {
        Set<String> ignoreGroupsSet = new HashSet<String>();
        for (String group : ignoreGroups) {
            ignoreGroupsSet.add(group.toLowerCase());
        }
        this.ignoreGroups = ignoreGroupsSet;
    }

    /**
     * @return
     */
    public String[] getDestroyActions() {
        return destroyActions;
    }

    /**
     * @param actions
     */
    public void setDestroyActions(String[] actions) {
        this.destroyActions = actions;
    }

    /**
     * @return
     */
    public String[] getDestroyWithActions() {
        return destroyWithActions;
    }

    /**
     * @param action
     */
    public void setDestroyWithActions(String[] actions) {
        this.destroyWithActions = actions;
    }

    /**
     * @return the rightClickActions
     */
    public String[] getCreateActions() {
        return createActions;
    }

    /**
     * @param actions
     */
    public void setCreateActions(String[] actions) {
        this.createActions = actions;
    }

    /**
     * @return
     */
    public String[] getUseActions() {
        return useActions;
    }

    /**
     * @param actions
     */
    public void setUseActions(String[] actions) {
        this.useActions = actions;
    }

    /**
     * @return
     */
    public String[] getDropActions() {
        return dropActions;
    }

    /**
     * @param actions
     */
    public void setDropActions(String[] actions) {
        this.dropActions = actions;
    }

    /**
     * Returns true if this player should be ignored.
     *
     * @param player
     * @return
     */
    public boolean shouldIgnore(Player player) {
        if (ignoreGroups == null) {
            return false;
        }
        for (String group : player.getGroups()) {
            if (ignoreGroups.contains(group.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Announces a message to all administrators.
     * 
     * @param str
     */
    public void notifyAdmins(String str) {
        for (Player player : etc.getServer().getPlayerList()) {
            if (player.canUseCommand("/wprotectalerts")
                    || player.canUseCommand("/worldguardnotify")) {
                player.sendMessage(Colors.LightPurple + "WorldGuard: " + str);
            }
        }
    }

    /**
     * Ban a player.
     * 
     * @param player
     * @param msg
     */
    public void banPlayer(Player player, String msg) {
        etc.getServer().ban(player.getName());
        etc.getLoader().callHook(PluginLoader.Hook.BAN, new Object[]{
            player.getUser(), player.getUser(), msg
        });
        player.kick(msg);
    }

    /**
     * Called on block destruction. Returns true to let the action pass
     * through.
     *
     * @param block
     * @param player
     * @return
     */
    public boolean onDestroy(final Block block, final Player player) {
        if (destroyActions == null) {
            return true;
        }

        final BlacklistEntry entry = this;

        ActionHandler handler = new ActionHandler() {
            public void log(String itemName) {
                blacklist.getLogger().logDestroyAttempt(player, block);
            }
            public void kick(String itemName) {
                player.kick("You are not allowed to destroy " + itemName);
            }
            public void ban(String itemName) {
                entry.banPlayer(player, "Banned: You are not allowed to destroy " + itemName);
            }
            public void notifyAdmins(String itemName) {
                entry.notifyAdmins(player.getName() + " tried to destroy " + itemName + ".");
            }
            public void tell(String itemName) {
                player.sendMessage(Colors.Yellow + "You are not allowed to destroy " + itemName + ".");
            }
        };

        return process(block.getType(), player, destroyActions, handler);
    }

    /**
     * Called on left click. Returns true to let the action pass through.
     *
     * @param item
     * @param player
     * @return
     */
    public boolean onDestroyWith(final int item, final Player player) {
        if (destroyWithActions == null) {
            return true;
        }

        final BlacklistEntry entry = this;

        ActionHandler handler = new ActionHandler() {
            public void log(String itemName) {
                blacklist.getLogger().logDestroyWithAttempt(player, item);
            }
            public void kick(String itemName) {
                player.kick("You can't destroy with " + itemName);
            }
            public void ban(String itemName) {
                entry.banPlayer(player, "Banned: You can't destroy with " + itemName);
            }
            public void notifyAdmins(String itemName) {
                entry.notifyAdmins(player.getName() + " tried to destroyed with " + itemName + ".");
            }
            public void tell(String itemName) {
                player.sendMessage(Colors.Yellow + "You can't destroy with " + itemName + ".");
            }
        };

        return process(item, player, destroyWithActions, handler);
    }

    /**
     * Called on right click. Returns true to let the action pass through.
     *
     * @param item
     * @param player
     * @return
     */
    public boolean onCreate(final int item, final Player player) {
        if (createActions == null) {
            return true;
        }

        final BlacklistEntry entry = this;

        ActionHandler handler = new ActionHandler() {
            public void log(String itemName) {
                blacklist.getLogger().logCreateAttempt(player, item);
            }
            public void kick(String itemName) {
                player.kick("You can't create " + itemName);
            }
            public void ban(String itemName) {
                entry.banPlayer(player, "Banned: You can't create " + itemName);
            }
            public void notifyAdmins(String itemName) {
                entry.notifyAdmins(player.getName() + " tried to create " + itemName + ".");
            }
            public void tell(String itemName) {
                player.sendMessage(Colors.Yellow + "You can't create " + itemName + ".");
            }
        };

        return process(item, player, createActions, handler);
    }

    /**
     * Called on right click upon. Returns true to let the action pass through.
     *
     * @param item
     * @param player
     * @return
     */
    public boolean onUse(final Block block, final Player player) {
        if (useActions == null) {
            return true;
        }

        final BlacklistEntry entry = this;

        ActionHandler handler = new ActionHandler() {
            public void log(String itemName) {
                blacklist.getLogger().logUseAttempt(player, block);
            }
            public void kick(String itemName) {
                player.kick("You can't use " + itemName);
            }
            public void ban(String itemName) {
                entry.banPlayer(player, "Banned: You can't use " + itemName);
            }
            public void notifyAdmins(String itemName) {
                entry.notifyAdmins(player.getName() + " tried to use " + itemName + ".");
            }
            public void tell(String itemName) {
                player.sendMessage(Colors.Yellow + "You're not allowed to use " + itemName + ".");
            }
        };

        return process(block.getType(), player, useActions, handler);
    }

    /**
     * Called on right click upon. Returns true to let the action pass through.
     *
     * @param item
     * @param player
     * @return
     */
    public boolean onSilentUse(final Block block, final Player player) {
        if (useActions == null) {
            return true;
        }

        ActionHandler handler = new ActionHandler() {
            public void log(String itemName) {
            }
            public void kick(String itemName) {
            }
            public void ban(String itemName) {
            }
            public void notifyAdmins(String itemName) {
            }
            public void tell(String itemName) {
            }
        };

        return process(block.getType(), player, useActions, handler);
    }

    /**
     * Called on item drop. Returns true to let the action pass through.
     *
     * @param item
     * @param player
     * @return
     */
    public boolean onDrop(final int item, final Player player) {
        if (dropActions == null) {
            return true;
        }

        final BlacklistEntry entry = this;

        ActionHandler handler = new ActionHandler() {
            public void log(String itemName) {
                blacklist.getLogger().logDropAttempt(player, item);
            }
            public void kick(String itemName) {
                player.kick("You can't drop " + itemName);
            }
            public void ban(String itemName) {
                entry.banPlayer(player, "Banned: You can't drop " + itemName);
            }
            public void notifyAdmins(String itemName) {
                entry.notifyAdmins(player.getName() + " tried to drop " + itemName + ".");
            }
            public void tell(String itemName) {
                player.sendMessage(Colors.Yellow + "You're not allowed to drop " + itemName + ".");
            }
        };

        return process(item, player, dropActions, handler);
    }

    /**
     * Internal method to handle the actions.
     * 
     * @param id
     * @param player
     * @param actions
     * @return
     */
    private boolean process(int id, Player player, String[] actions, ActionHandler handler) {
        if (shouldIgnore(player)) {
            return true;
        }

        String name = player.getName();
        long now = System.currentTimeMillis();
        boolean repeating = false;

        // Check to see whether this event is being repeated
        BlacklistTrackedEvent tracked = lastAffected.get(name);
        if (tracked != null) {
            if (tracked.getId() == id && tracked.getTime() > now - 3000) {
                repeating = true;
            } else {
                tracked.setTime(now);
                tracked.setId(id);
            }
        } else {
            lastAffected.put(name, new BlacklistTrackedEvent(id, now));
        }

        boolean ret = true;
        
        for (String action : actions) {
            if (action.equalsIgnoreCase("deny")) {
                ret = false;
            } else if (action.equalsIgnoreCase("kick")) {
                handler.kick(etc.getDataSource().getItem(id));
            } else if (action.equalsIgnoreCase("ban")) {
                handler.ban(etc.getDataSource().getItem(id));
            } else if (!repeating) {
                if (action.equalsIgnoreCase("notify")) {
                    handler.notifyAdmins(etc.getDataSource().getItem(id));
                } else if (action.equalsIgnoreCase("log")) {
                    handler.log(etc.getDataSource().getItem(id));
                } else if (!repeating && action.equalsIgnoreCase("tell")) {
                    handler.tell(etc.getDataSource().getItem(id));
                }
            }
        }

        return ret;
    }

    /**
     * Forget a player.
     *
     * @param player
     */
    public static void forgetPlayer(Player player) {
        lastAffected.remove(player.getName());
    }

    /**
     * Forget all players.
     *
     * @param player
     */
    public static void forgetAllPlayers() {
        lastAffected.clear();
    }

    /**
     * Gets called for actions.
     */
    private static interface ActionHandler {
        public void log(String itemName);
        public void kick(String itemName);
        public void ban(String itemName);
        public void notifyAdmins(String itemName);
        public void tell(String itemName);
    }
}
