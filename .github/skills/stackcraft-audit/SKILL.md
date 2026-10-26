---
name: stackcraft-audit
description: Checks RetroFlow against StackCraft's rules for service-layer exception messages and the HTTP status for business-rule violations, using the live standards document from the MCP server. Use it when someone asks which exception message format or business-rule status code StackCraft requires, or whether the code follows those rules.
---
# StackCraft audit: exception messages and business-rule status

## Sources (in this order)
1. **MCP, the only source of truth:** `ai-track-stan` → `read_document("stackcraft-coding")`. Note the version. Read §2.1, §4.1, and for context §2.2 and §4.2.
2. **Code:** `exception/`, every `throw new` in `service/`, and the status mappings in `GlobalExceptionHandler`.
3. **`decisions.md`:** context only (for example `new` as the identifier, D3 = 403). It never replaces the standard.

## Steps
1. If the MCP tools are missing or the call fails, **stop**. Say so (enable the tools, start a new chat). Don't answer from memory.
2. Quote §2.1 word for word: the format string, one example, the reason and the severity.
3. Quote the "Business rule violation" row of §4.1.
4. Take the severity from the `Violation severity:` line, not the heading (they can differ, as in §1.1).
5. Check that every service message matches `<EntityType> <identifier>: <reason>` and that business-rule exceptions map to 409.
6. Open every file you cite and check the line number. Report deviations, and name the decision in `decisions.md` if one covers them.

## Output format
```markdown
### StackCraft rule (`stackcraft-coding` vX.Y, through MCP)
**§2.1 exception format (severity: …)**: > `<exact format>`. Example: `<quoted>`. Why: <reason>
**§4.1 business-rule violation (severity: …)**: > <status from the document>
### RetroFlow check
| Rule | ✅/❌ | file:line | Note |
|---|---|---|---|
### Deviations
- file:line – the problem – the fix (or "none")
```
Then add the prompt to `prompt-log.md` with a verdict.

## Never
- Answer from memory, an old copy or `decisions.md` alone.
- Quote text that isn't in the document, or paraphrase the format string.
- Give a file or line number you haven't opened and checked.
- Change code or tests (this skill is read-only), or overrule `decisions.md` on your own.
