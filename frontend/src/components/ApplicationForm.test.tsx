import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApplicationForm } from './ApplicationForm'
import { EMPTY_FIELDS, validate } from './applicationFields'

describe('ApplicationForm', () => {
  it('submits what the user typed', async () => {
    const onSubmit = vi.fn()
    render(<ApplicationForm submitLabel="Add" onSubmit={onSubmit} />)

    await userEvent.type(screen.getByLabelText(/Company/), 'Acme')
    await userEvent.type(screen.getByLabelText(/Role/), 'Backend Engineer')
    await userEvent.type(screen.getByLabelText('Job posting URL'), 'https://acme.example/jobs/1')
    await userEvent.click(screen.getByRole('button', { name: 'Add' }))

    expect(onSubmit).toHaveBeenCalledWith({
      ...EMPTY_FIELDS,
      company: 'Acme',
      roleTitle: 'Backend Engineer',
      jobUrl: 'https://acme.example/jobs/1',
    })
  })

  it('shows what is missing and does not submit', async () => {
    const onSubmit = vi.fn()
    render(<ApplicationForm submitLabel="Add" onSubmit={onSubmit} />)

    await userEvent.click(screen.getByRole('button', { name: 'Add' }))

    expect(screen.getByText('Company is required')).toBeInTheDocument()
    expect(screen.getByText('Role is required')).toBeInTheDocument()
    expect(screen.getByLabelText(/Company/)).toHaveAttribute('aria-invalid', 'true')
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('shows field errors that came from the server', () => {
    render(<ApplicationForm submitLabel="Save" serverErrors={{ jobUrl: 'must start with http:// or https://' }} onSubmit={vi.fn()} />)

    expect(screen.getByText('must start with http:// or https://')).toBeInTheDocument()
  })

  it('starts from the values it is given', () => {
    render(<ApplicationForm submitLabel="Save" initial={{ ...EMPTY_FIELDS, company: 'Globex' }} onSubmit={vi.fn()} />)

    expect(screen.getByLabelText(/Company/)).toHaveValue('Globex')
  })
})

describe('validate', () => {
  const valid = { ...EMPTY_FIELDS, company: 'Acme', roleTitle: 'Engineer' }

  it('accepts a minimal application', () => {
    expect(validate(valid)).toEqual({})
  })

  it('rejects a URL that is not http or https', () => {
    expect(validate({ ...valid, jobUrl: 'ftp://acme.example' })).toHaveProperty('jobUrl')
    expect(validate({ ...valid, jobUrl: 'acme.example' })).toHaveProperty('jobUrl')
  })

  it('rejects an application date in the future', () => {
    expect(validate({ ...valid, appliedOn: '2999-01-01' })).toHaveProperty('appliedOn')
    expect(validate({ ...valid, appliedOn: '2020-01-01' })).toEqual({})
  })
})
