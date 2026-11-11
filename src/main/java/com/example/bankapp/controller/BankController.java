package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Controller (not @RestController): each method returns the NAME of a Thymeleaf template
 * in src/main/resources/templates (bank.html, history.html, ...).
 */
@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    // ===================== Dashboard =====================

    // GET /            -> dashboard
    // GET /?keyword=sil -> search customers by name
    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
        loadDashboard(model, keyword);
        return "bank";
    }

    // ===================== Transfer =====================

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        // Bean validation failed (amount empty / 0 ...) -> show messages, NOTHING is saved
        if (bindingResult.hasErrors()) {
            model.addAttribute("customer", new Customer());
            model.addAttribute("error", "Please correct the errors in the transfer form.");
            loadDashboard(model, null);
            return "bank";
        }

        try {
            // The whole transfer runs inside ONE transaction (BankService.transfer)
            BankTransaction tx = bankService.transfer(request.getFromAccount(), request.getToAccount(),
                    request.getAmount(), request.isSimulateFailure());
            redirectAttributes.addFlashAttribute("success",
                    "Transfer successful: LKR " + tx.getAmount() + " from "
                            + tx.getFromAccount() + " to " + tx.getToAccount());
        } catch (Exception e) {
            // When we arrive here, transfer() has ALREADY been rolled back.
            // Record the failure in a NEW transaction (REQUIRES_NEW) so it is not lost.
            auditService.recordFailure(request.getFromAccount(), request.getToAccount(),
                    request.getAmount(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Transfer failed: " + e.getMessage());
        }
        return "redirect:/";
    }

    // ===================== Add customer =====================

    @PostMapping("/customers")
    public String addCustomer(@Valid @ModelAttribute("customer") Customer customer,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("transferRequest", new TransferRequest());
            model.addAttribute("error", "Please correct the errors in the customer form.");
            loadDashboard(model, null);
            return "bank";
        }

        try {
            Customer saved = bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("success",
                    "Customer " + saved.getFullName() + " (" + saved.getAccountNumber() + ") was added");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage()); // e.g. "already exists"
        }
        return "redirect:/";
    }

    // ===================== Account history =====================

    // GET /accounts/100001/history
    @GetMapping("/accounts/{accountNumber}/history")
    public String history(@PathVariable String accountNumber, Model model,
                          RedirectAttributes redirectAttributes) {
        Customer customer;
        try {
            customer = bankService.getCustomer(accountNumber);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
        model.addAttribute("customer", customer);
        model.addAttribute("transactions", bankService.getHistory(accountNumber));
        model.addAttribute("totalSent", bankService.getTotalSent(accountNumber)); // Task 12
        return "history";
    }

    // ===================== Task 12 =====================

    // GET /large-transfers?min=10000
    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers", bankService.getLargeTransfers(min));
        return "large-transfers";
    }

    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", customersForCashForms());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam String accountNumber,
                          @RequestParam(required = false) BigDecimal amount,
                          RedirectAttributes redirectAttributes) {
        try {
            bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success",
                    "LKR " + amount + " was deposited to account " + accountNumber);
        } catch (Exception e) {
            auditService.recordFailure(null, accountNumber, amount, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Deposit failed: " + e.getMessage());
        }
        return "redirect:/deposit";
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", customersForCashForms());
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String accountNumber,
                           @RequestParam(required = false) BigDecimal amount,
                           RedirectAttributes redirectAttributes) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success",
                    "LKR " + amount + " was withdrawn from account " + accountNumber);
        } catch (Exception e) {
            auditService.recordFailure(accountNumber, null, amount, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Withdrawal failed: " + e.getMessage());
        }
        return "redirect:/withdraw";
    }

    // ===================== Helpers =====================

    /** Everything bank.html needs. */
    private void loadDashboard(Model model, String keyword) {
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("customers", bankService.getCustomers(keyword));   // table (search result)
        model.addAttribute("allCustomers", bankService.getAllCustomers());    // dropdowns
        model.addAttribute("totalCustomers", bankService.getCustomerCount()); // native SQL COUNT
        model.addAttribute("totalBalance", bankService.getTotalBalance());    // JPQL SUM
        model.addAttribute("successCount", bankService.countByStatus(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankService.countByStatus(TransactionStatus.FAILED));
        model.addAttribute("transactions", bankService.getLatestTransactions());
        model.addAttribute("feeEnabled", bankService.isFeeEnabled());
    }

    /** Customers for the deposit / withdraw dropdowns (the bank's fee account is hidden). */
    private List<Customer> customersForCashForms() {
        return bankService.getAllCustomers().stream()
                .filter(c -> !"999999".equals(c.getAccountNumber()))
                .toList();
    }
}
