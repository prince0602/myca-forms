package com.mynx.myca500.patient_forms.admin;

import com.mynx.myca500.patient_forms.enums.BillingInterval;
import com.mynx.myca500.patient_forms.enums.Plan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;
import java.util.Locale;


@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "usage-admin", name = "command")
public class UsageAdminCommandRunner implements ApplicationRunner {

    static final String USAGE = """
            Commands (all except 'show' require --reason and --operator=<your app user email>):
              show                --workspace=K7M2QX
              set-plan-default    --plan=FREE|PLUS --limit=N
              set-custom-limit    --workspace=K7M2QX --limit=N
              remove-custom-limit --workspace=K7M2QX
              grant-bonus         --workspace=K7M2QX --amount=N
              set-plan            --workspace=K7M2QX --plan=FREE|PLUS [--interval=MONTHLY|ANNUAL]
            """;

    private final UsageAdminService adminService;
    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        int exitCode;
        try {
            System.out.println(execute(args));
            exitCode = 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage() + System.lineSeparator() + USAGE);
            exitCode = 2;
        } catch (RuntimeException e) {
            log.error("usage-admin command failed", e);
            exitCode = 1;
        }
        int code = exitCode;
        System.exit(SpringApplication.exit(context, () -> code));
    }

    String execute(ApplicationArguments args) {
        String command = required(args, "usage-admin.command");
        if ("show".equals(command)) {
            return adminService.show(required(args, "workspace"));
        }
        AdminOperator operator = adminService.resolveOperator(
                optional(args, "operator"), optional(args, "reason"), System.getProperty("user.name"), hostName());
        return switch (command) {
            case "set-plan-default" -> adminService.setPlanDefault(plan(args), intArg(args, "limit"), operator);
            case "set-custom-limit" -> adminService.setCustomLimit(required(args, "workspace"), intArg(args, "limit"), operator);
            case "remove-custom-limit" -> adminService.removeCustomLimit(required(args, "workspace"), operator);
            case "grant-bonus" -> adminService.grantBonus(required(args, "workspace"), intArg(args, "amount"), operator);
            case "set-plan" -> adminService.setPlan(required(args, "workspace"), plan(args), interval(args), operator);
            default -> throw new IllegalArgumentException("Unknown command '" + command + "'");
        };
    }

    private static Plan plan(ApplicationArguments args) {
        return parseEnum(Plan.class, required(args, "plan"), "--plan");
    }

    private static BillingInterval interval(ApplicationArguments args) {
        String value = optional(args, "interval");
        return value == null ? null : parseEnum(BillingInterval.class, value, "--interval");
    }

    private static int intArg(ApplicationArguments args, String name) {
        try {
            return Integer.parseInt(required(args, name));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--" + name + " must be a whole number");
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String name) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(name + " must be one of " + List.of(type.getEnumConstants()));
        }
    }

    private static String required(ApplicationArguments args, String name) {
        String value = optional(args, name);
        if (value == null) {
            throw new IllegalArgumentException("--" + name + " is required");
        }
        return value;
    }

    private static String optional(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        if (values == null || values.isEmpty() || values.get(0).isBlank()) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalArgumentException("--" + name + " given more than once");
        }
        return values.get(0).trim();
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
