# HULK Lean Execution Rules

## Repository role
BLUE ON app. Preserve separation from BLUECORE; consume domain outputs rather than duplicating prescription rules.

## Work efficiently
- Search narrowly before opening files.
- Read only the files needed for the current task.
- Do not reread unchanged files or replay history already captured in handoffs.
- Make the smallest safe change.
- Run narrow tests first; broaden only when justified.
- Reuse existing utilities/components before creating new abstractions.

## Model/quota policy
- Routine or bulk work: GPT-6 Luna / low reasoning.
- Normal coding and analysis: GPT-6 Sol / low reasoning.
- Escalate reasoning/model only after a concrete failure or genuinely difficult requirement.
- Astra is not a default.
- Fast mode is not a default.
- Keep long task state in concise handoffs/checkpoints.

## Safety
- Never commit secrets.
- Ask before publishing, spending, deleting, production deployment, sending external messages, or another irreversible/high-impact action.
- Prefer deterministic validation (tests, lint, build, schemas) before requesting another model review.
