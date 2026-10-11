package com.fivesoce.mira.fp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/** Admin-only hybrid foundation using explicitly simulated stand-in entities. */
public final class MiraFP extends JavaPlugin implements TabExecutor, Listener {
    private static final String GUI = "Mira-FP Administration";
    private static final String KEY = "^[A-Za-z0-9_]{1,16}$";
    private final Map<String, Actor> actors = new LinkedHashMap<>();
    private File storeFile;
    private BukkitTask ticker;

    private static final class Actor {
        String name, world, rank = "", tag = "", mode = "idle";
        double x,y,z, radius = 8;
        String held = "AIR", helmet = "AIR", chest = "AIR", legs = "AIR", boots = "AIR";
        String[] inventory = new String[36];
        ArmorStand entity;
        long lastChat;
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        storeFile = new File(getDataFolder(), "actors.yml");
        loadActors();
        Objects.requireNonNull(getCommand("fp")).setExecutor(this);
        Objects.requireNonNull(getCommand("fp")).setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        for (Actor actor : actors.values()) spawn(actor);
        long period = Math.max(10, getConfig().getLong("movement.interval-ticks",30));
        ticker = Bukkit.getScheduler().runTaskTimer(this, this::tick, period, period);
        getLogger().info("Mira-FP foundation enabled. Stand-in models only; no actual player/tablist identities yet.");
    }

    @Override public void onDisable() {
        if (ticker != null) ticker.cancel();
        saveActors();
        for (Actor actor : actors.values()) despawn(actor);
    }

    private void loadActors() {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(storeFile);
        ConfigurationSection root = data.getConfigurationSection("actors");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            if (!key.matches(KEY)) continue;
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            Actor a = new Actor();
            a.name = key;
            a.world = sec.getString("world", "");
            a.x = sec.getDouble("x"); a.y = sec.getDouble("y"); a.z = sec.getDouble("z");
            a.radius = Math.max(0, Math.min(128, sec.getDouble("radius", 8)));
            a.rank = sec.getString("rank",""); a.tag = sec.getString("tag","");
            a.mode = sec.getString("mode","idle");
            if (!List.of("idle","wander").contains(a.mode)) a.mode = "idle";
            a.held = sec.getString("held","AIR");
            a.helmet = sec.getString("helmet","AIR");
            a.chest = sec.getString("chest","AIR");
            a.legs = sec.getString("legs","AIR");
            a.boots = sec.getString("boots","AIR");
            List<String> slots = sec.getStringList("inventory");
            for (int i=0; i<Math.min(36,slots.size());i++) a.inventory[i] = slots.get(i);
            actors.put(key.toLowerCase(Locale.ROOT), a);
        }
    }

    private void saveActors() {
        YamlConfiguration data = new YamlConfiguration();
        for (Actor a : actors.values()) {
            String p = "actors."+a.name+".";
            data.set(p+"world",a.world);
            data.set(p+"x",a.x); data.set(p+"y",a.y); data.set(p+"z",a.z);
            data.set(p+"radius",a.radius); data.set(p+"rank",a.rank); data.set(p+"tag",a.tag);
            data.set(p+"mode",a.mode); data.set(p+"held",a.held);
            data.set(p+"helmet",a.helmet); data.set(p+"chest",a.chest);
            data.set(p+"legs",a.legs); data.set(p+"boots",a.boots);
            data.set(p+"inventory",Arrays.asList(a.inventory));
        }
        try { data.save(storeFile); }
        catch (IOException e) { getLogger().log(Level.SEVERE, "Cannot persist actor profiles!", e); }
    }

    private String display(Actor a) {
        return ChatColor.translateAlternateColorCodes('&', a.rank+" "+a.name+" "+a.tag).trim();
    }

    private void spawn(Actor a) {
        if (a.entity != null && a.entity.isValid()) return;
        World world = Bukkit.getWorld(a.world);
        if (world == null) return;
        Location at = new Location(world,a.x,a.y,a.z);
        a.entity = (ArmorStand) world.spawnEntity(at,EntityType.ARMOR_STAND);
        a.entity.setGravity(false);
        a.entity.setArms(true);
        a.entity.setBasePlate(false);
        a.entity.setCustomName(display(a));
        a.entity.setCustomNameVisible(true);
        a.entity.setInvulnerable(true);
        a.entity.setPersistent(false);
        applyGear(a);
    }

    private void despawn(Actor a) {
        if (a.entity != null) { a.entity.remove(); a.entity = null; }
    }

    private static ItemStack item(String raw) {
        Material mat = Material.matchMaterial(raw == null ? "AIR" : raw);
        return mat == null ? new ItemStack(Material.AIR) : new ItemStack(mat);
    }

    private void applyGear(Actor a) {
        if (a.entity == null) return;
        a.entity.getEquipment().setItemInMainHand(item(a.held));
        a.entity.getEquipment().setHelmet(item(a.helmet));
        a.entity.getEquipment().setChestplate(item(a.chest));
        a.entity.getEquipment().setLeggings(item(a.legs));
        a.entity.getEquipment().setBoots(item(a.boots));
    }

    private void tick() {
        double step = Math.max(0,Math.min(3,getConfig().getDouble("movement.wander-step",1.5)));
        for (Actor a : actors.values()) {
            if (a.entity == null || !a.entity.isValid() || !a.mode.equals("wander")) continue;
            World world = Bukkit.getWorld(a.world);
            if (world == null) continue;
            Location at = a.entity.getLocation();
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI*2);
            double nx = at.getX() + Math.cos(angle)*step;
            double nz = at.getZ() + Math.sin(angle)*step;
            if (Math.hypot(nx-a.x,nz-a.z)>a.radius) continue;
            int blockY = at.getBlockY();
            if (!world.getBlockAt((int)Math.floor(nx),blockY,(int)Math.floor(nz)).isPassable()) continue;
            if (!world.getBlockAt((int)Math.floor(nx),blockY+1,(int)Math.floor(nz)).isPassable()) continue;
            if (world.getBlockAt((int)Math.floor(nx),blockY-1,(int)Math.floor(nz)).isPassable()) continue;
            a.entity.teleport(new Location(world,nx,at.getY(),nz,(float)Math.toDegrees(angle)-90,0));
        }
    }

    private void menu(Player player) {
        Inventory inv = Bukkit.createInventory(null,27,GUI);
        inv.setItem(11, labelled(Material.PLAYER_HEAD,ChatColor.GOLD+"Simulated Actors: "+actors.size()));
        inv.setItem(13, labelled(Material.COMPASS,ChatColor.YELLOW+"Movement: idle / wander"));
        inv.setItem(15, labelled(Material.BOOK,ChatColor.AQUA+"Use /fp help for admin commands"));
        player.openInventory(inv);
    }

    private static ItemStack labelled(Material mat,String name) {
        ItemStack stack = new ItemStack(mat);
        var meta = stack.getItemMeta();
        if (meta != null) {meta.setDisplayName(name);stack.setItemMeta(meta);}
        return stack;
    }

    @EventHandler public void menuClick(InventoryClickEvent e) {
        if (GUI.equals(e.getView().getTitle())) e.setCancelled(true);
    }

    private void say(CommandSender admin,String msg) {admin.sendMessage(ChatColor.GOLD+"[Mira-FP] "+ChatColor.RESET+msg);}
    private Actor find(CommandSender admin,String name) {
        Actor a = actors.get(name.toLowerCase(Locale.ROOT));
        if (a==null) say(admin,ChatColor.RED+"Unknown fake actor: "+name);
        return a;
    }
    private static String joined(String[] args,int from) {return String.join(" ",Arrays.copyOfRange(args,from,args.length));}

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if (!sender.hasPermission("mirafp.admin")) {sender.sendMessage(ChatColor.RED+"No permission.");return true;}
        if (args.length==0) {
            if (sender instanceof Player p) menu(p);
            else say(sender,"Use /fp help.");
            return true;
        }
        String verb = args[0].toLowerCase(Locale.ROOT);
        if (verb.equals("help")) {
            say(sender,"create <name>, remove <name>, list, spawn <name>, despawn <name>, tp <name>, mode <name> idle|wander, radius <name> <blocks>, gear <name> hand|helmet|chest|legs|boots <material>, rank <name> <text>, tag <name> <text>, chat <name> <message>, run <name> <action-id>, reload");
            return true;
        }
        if (verb.equals("reload")) {reloadConfig();say(sender,"Configuration reloaded.");return true;}
        if (verb.equals("list")) {
            say(sender,actors.isEmpty()?"No actors created.":String.join(", ",actors.values().stream().map(a->a.name).toList()));
            return true;
        }
        if (args.length<2) {say(sender,"Use /fp help.");return true;}
        if (verb.equals("create")) {
            if (!(sender instanceof Player p)) {say(sender,"Create in-game for a spawn location.");return true;}
            if (!args[1].matches(KEY)) {say(sender,"Names must be 1-16 letters, numbers or underscores.");return true;}
            String id = args[1].toLowerCase(Locale.ROOT);
            if (actors.containsKey(id)) {say(sender,"Already exists.");return true;}
            Actor a = new Actor();
            Location loc = p.getLocation();
            a.name=args[1];a.world=loc.getWorld().getName();a.x=loc.getX();a.y=loc.getY();a.z=loc.getZ();
            a.radius=getConfig().getDouble("movement.default-radius",8);
            actors.put(id,a);saveActors();spawn(a);say(sender,"Created stand-in actor "+a.name+".");return true;
        }
        Actor a = find(sender,args[1]);
        if (a==null) return true;
        switch (verb) {
            case "remove" -> {despawn(a);actors.remove(a.name.toLowerCase(Locale.ROOT));saveActors();say(sender,"Removed "+a.name);}
            case "spawn" -> {spawn(a);say(sender,"Spawn requested.");}
            case "despawn" -> {despawn(a);say(sender,"Despawned.");}
            case "tp" -> {
                if (!(sender instanceof Player p)) {say(sender,"Player only.");break;}
                Location loc=p.getLocation();despawn(a);
                a.world=loc.getWorld().getName();a.x=loc.getX();a.y=loc.getY();a.z=loc.getZ();
                saveActors();spawn(a);say(sender,"Teleported actor to your location.");
            }
            case "mode" -> {
                if (args.length<3 || !List.of("idle","wander").contains(args[2].toLowerCase(Locale.ROOT))) {say(sender,"Use idle or wander.");break;}
                a.mode=args[2].toLowerCase(Locale.ROOT);saveActors();say(sender,"Movement mode set.");
            }
            case "radius" -> {
                if (args.length<3) {say(sender,"Specify a radius (0-128).");break;}
                try {double r=Double.parseDouble(args[2]);if (!Double.isFinite(r)||r<0||r>128) throw new NumberFormatException();a.radius=r;saveActors();say(sender,"Movement radius set.");}
                catch(NumberFormatException e){say(sender,"Radius must be 0-128.");}
            }
            case "rank","tag" -> {
                if (args.length<3) {say(sender,"Supply display text.");break;}
                String value=joined(args,2);
                if (value.length()>64) {say(sender,"Maximum 64 characters.");break;}
                if (verb.equals("rank")) a.rank=value; else a.tag=value;
                if (a.entity!=null) a.entity.setCustomName(display(a));
                saveActors();say(sender,"Display updated.");
            }
            case "gear" -> {
                if (args.length!=4) {say(sender,"Usage: /fp gear <name> hand|helmet|chest|legs|boots <material>");break;}
                Material m=Material.matchMaterial(args[3]);
                if (m==null||!m.isItem()) {say(sender,"Unknown item material.");break;}
                switch(args[2].toLowerCase(Locale.ROOT)) {
                    case "hand" -> a.held=m.name();
                    case "helmet" -> a.helmet=m.name();
                    case "chest" -> a.chest=m.name();
                    case "legs" -> a.legs=m.name();
                    case "boots" -> a.boots=m.name();
                    default -> {say(sender,"Unknown equipment slot.");return true;}
                }
                applyGear(a);saveActors();say(sender,"Equipment updated.");
            }
            case "chat" -> {
                if (args.length<3) {say(sender,"Supply a message.");break;}
                long cooldown=Math.max(1,getConfig().getLong("chat.cooldown-seconds",30))*1000L;
                if (System.currentTimeMillis()-a.lastChat<cooldown) {say(sender,"Chat cooldown active.");break;}
                String message=joined(args,2);
                if (message.length()>200) {say(sender,"Message too long.");break;}
                a.lastChat=System.currentTimeMillis();
                String prefix=ChatColor.translateAlternateColorCodes('&',getConfig().getString("chat.prefix","[Simulated] "));
                Bukkit.broadcastMessage(prefix+display(a)+ChatColor.WHITE+": "+message);
            }
            case "run" -> {
                if (args.length!=3||!args[2].matches("[A-Za-z0-9_-]{1,40}")) {say(sender,"Usage: /fp run <name> <action-id>");break;}
                String commandLine=getConfig().getString("actions."+args[2]);
                if (commandLine==null||commandLine.isBlank()||commandLine.contains("\n")||commandLine.contains("\r")) {say(sender,"Action not allowlisted.");break;}
                // Explicitly configured commands only; never substitute arbitrary user input.
                boolean ok=Bukkit.dispatchCommand(Bukkit.getConsoleSender(),commandLine.replace("{actor}",a.name));
                say(sender,ok?"Configured action dispatched.":"Action was not handled.");
            }
            default -> say(sender,"Use /fp help.");
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        if (!sender.hasPermission("mirafp.admin")) return List.of();
        if (args.length==1) return List.of("help","create","remove","list","spawn","despawn","tp","mode","radius","gear","rank","tag","chat","run","reload");
        if (args.length==2) return new ArrayList<>(actors.values().stream().map(a->a.name).toList());
        if (args.length==3 && args[0].equalsIgnoreCase("mode")) return List.of("idle","wander");
        if (args.length==3 && args[0].equalsIgnoreCase("gear")) return List.of("hand","helmet","chest","legs","boots");
        ConfigurationSection section=getConfig().getConfigurationSection("actions");
        if (args.length==3&&args[0].equalsIgnoreCase("run")&&section!=null) return new ArrayList<>(section.getKeys(false));
        return List.of();
    }
}
