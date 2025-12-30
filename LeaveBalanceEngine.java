import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;

public class LeaveBalanceEngine {

    public static class EntitlementResult {
        public final int baseEntitlement;
        public final int proratedEntitlement;

        public EntitlementResult(int baseEntitlement, int proratedEntitlement) {
            this.baseEntitlement = baseEntitlement;
            this.proratedEntitlement = proratedEntitlement;
        }
    }

    /**
     * Dasar kelayakan cuti berdasarkan undang-undang dan polisi syarikat.
     * - MATERNITY: 98 hari (Kelayakan: Kerja > 90 hari).
     * - PATERNITY: 7 hari (Kelayakan: Kerja > 12 bulan / 365 hari).
     */
    public static int baseEntitlementByType(String typeCode, long serviceYears, String genderUpper, long totalDaysEmployed) {
        String t = typeCode == null ? "" : typeCode.trim().toUpperCase();
        String g = genderUpper == null ? "" : genderUpper.trim().toUpperCase();

        switch (t) {
            case "ANNUAL":
                if (serviceYears < 2) return 8;
                if (serviceYears < 5) return 12;
                return 16;

            case "SICK":
                if (serviceYears < 2) return 14;
                if (serviceYears < 5) return 18;
                return 22;

            case "HOSPITALIZATION":
                return 60;

            case "MATERNITY":
                // Kelayakan: Pekerja wanita yang telah bekerja sekurang-kurangnya 90 hari
                if ((g.equals("F") || g.equals("FEMALE")) && totalDaysEmployed >= 90) return 98;
                return 0;

            case "PATERNITY":
                // Kelayakan: Pekerja lelaki yang telah bekerja sekurang-kurangnya 12 bulan (365 hari)
                // Jika 0 days keluar, bermaksud pekerja belum cukup setahun bekerja.
                if ((g.equals("M") || g.equals("MALE")) && totalDaysEmployed >= 365) return 7;
                return 0;

            case "EMERGENCY":
                return 5;

            case "UNPAID":
                return 3;

            default:
                return 0;
        }
    }

    public static long serviceYears(LocalDate hireDate, LocalDate today) {
        if (hireDate == null || today.isBefore(hireDate)) return 0;
        return ChronoUnit.YEARS.between(hireDate, today);
    }

    public static int completedMonthsThisYear(LocalDate hireDate, LocalDate today) {
        if (hireDate == null) return 0;
        int year = today.getYear();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate start = hireDate.isAfter(yearStart) ? hireDate : yearStart;
        if (today.isBefore(start)) return 0;
        long months = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), today.withDayOfMonth(1));
        return (int) Math.min(12, Math.max(0, months));
    }

    public static EntitlementResult computeEntitlement(String typeCode, LocalDate hireDate, String genderUpper) {
        LocalDate today = LocalDate.now();
        long years = serviceYears(hireDate, today);
        long daysEmployed = (hireDate != null) ? ChronoUnit.DAYS.between(hireDate, today) : 0;
        
        int base = baseEntitlementByType(typeCode, years, genderUpper, daysEmployed);

        // Prorate hanya untuk Annual dan Sick jika baru mula tahun ini
        int currentYear = Year.now().getValue();
        boolean hiredThisYear = (hireDate != null && hireDate.getYear() == currentYear);
        String t = (typeCode != null) ? typeCode.trim().toUpperCase() : "";
        boolean isProratable = t.equals("ANNUAL") || t.equals("SICK");

        if (!hiredThisYear || !isProratable) {
            return new EntitlementResult(base, base);
        }

        int completedMonths = completedMonthsThisYear(hireDate, today);
        int prorated = (int) Math.floor((completedMonths / 12.0) * base);

        return new EntitlementResult(base, prorated);
    }

    public static double availableDays(int entitlement, int carriedFwd, double used, double pending) {
        return (entitlement + carriedFwd) - used - pending;
    }
}