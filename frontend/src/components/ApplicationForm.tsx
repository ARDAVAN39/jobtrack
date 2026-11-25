import { useState, type FormEvent } from 'react'
import type { ApplicationFields } from '../api/types'
import { EMPTY_FIELDS, validate } from './applicationFields'

interface Props {
  initial?: ApplicationFields
  submitLabel: string
  submitting?: boolean
  /** Errors from the server, keyed by field name. */
  serverErrors?: Record<string, string>
  onSubmit: (fields: ApplicationFields) => void
  onCancel?: () => void
}

/**
 * The form checks the obvious things itself so the user gets instant feedback. The server checks
 * everything again; its field errors are shown in the same place.
 */
export function ApplicationForm({ initial = EMPTY_FIELDS, submitLabel, submitting, serverErrors = {}, onSubmit, onCancel }: Props) {
  const [fields, setFields] = useState<ApplicationFields>(initial)
  const [errors, setErrors] = useState<Record<string, string>>({})

  const set = (name: keyof ApplicationFields) => (value: string) => setFields((current) => ({ ...current, [name]: value }))

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const found = validate(fields)
    setErrors(found)
    if (Object.keys(found).length === 0) {
      onSubmit(fields)
    }
  }

  const errorFor = (name: keyof ApplicationFields) => errors[name] ?? serverErrors[name]

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      <Field label="Company" name="company" value={fields.company} onChange={set('company')} error={errorFor('company')} required />
      <Field label="Role" name="roleTitle" value={fields.roleTitle} onChange={set('roleTitle')} error={errorFor('roleTitle')} required />
      <Field label="Location" name="location" value={fields.location} onChange={set('location')} error={errorFor('location')} />
      <Field label="Job posting URL" name="jobUrl" type="url" value={fields.jobUrl} onChange={set('jobUrl')} error={errorFor('jobUrl')} />
      <Field label="Applied on" name="appliedOn" type="date" value={fields.appliedOn} onChange={set('appliedOn')} error={errorFor('appliedOn')} />
      <label className="field">
        <span>Notes</span>
        <textarea name="notes" rows={4} value={fields.notes} onChange={(e) => set('notes')(e.target.value)} />
        {errorFor('notes') && <span className="field-error">{errorFor('notes')}</span>}
      </label>
      <div className="form-actions">
        <button type="submit" className="primary" disabled={submitting}>
          {submitting ? 'Saving…' : submitLabel}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}

interface FieldProps {
  label: string
  name: string
  value: string
  onChange: (value: string) => void
  error?: string
  type?: string
  required?: boolean
}

function Field({ label, name, value, onChange, error, type = 'text', required }: FieldProps) {
  const errorId = `${name}-error`
  return (
    <label className="field">
      <span>
        {label}
        {required && <span aria-hidden="true"> *</span>}
      </span>
      <input
        name={name}
        type={type}
        value={value}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        onChange={(e) => onChange(e.target.value)}
      />
      {error && (
        <span id={errorId} className="field-error">
          {error}
        </span>
      )}
    </label>
  )
}
