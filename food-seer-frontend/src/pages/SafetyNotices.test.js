import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import SafetyNotices from './SafetyNotices';
import { getMyNotifications, acknowledgeNotification } from '../services/api';

jest.mock('../services/api', () => ({
  getMyNotifications: jest.fn(),
  acknowledgeNotification: jest.fn(),
}));

const unreadNotice = {
  id: 1,
  incidentId: 3,
  orderId: 8,
  message: 'Food safety notice: your order #8 (COOKIES) may have been exposed to PEANUT.',
  createdAt: '2026-10-12T12:00:00',
  acknowledged: false,
  acknowledgedAt: null,
  remedy: 'Full refund or a free remake',
};

const renderPage = () => render(
  <MemoryRouter>
    <SafetyNotices />
  </MemoryRouter>
);

describe('SafetyNotices', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('shows a notice with the restaurant offer', async () => {
    getMyNotifications.mockResolvedValue([unreadNotice]);

    renderPage();

    expect(await screen.findByText(/may have been exposed to PEANUT/)).toBeInTheDocument();
    expect(screen.getByText('Full refund or a free remake')).toBeInTheDocument();
    expect(screen.getByText('⚠️ New')).toBeInTheDocument();
  });

  test('says the restaurant will follow up when there is no offer yet', async () => {
    getMyNotifications.mockResolvedValue([{ ...unreadNotice, remedy: null }]);

    renderPage();

    expect(await screen.findByText(/will follow up with an offer/)).toBeInTheDocument();
  });

  test('shows an empty state when there are no notices', async () => {
    getMyNotifications.mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText('You have no food safety notices.')).toBeInTheDocument();
  });

  test('acknowledging a notice calls the API and reloads', async () => {
    getMyNotifications
      .mockResolvedValueOnce([unreadNotice])
      .mockResolvedValueOnce([{ ...unreadNotice, acknowledged: true, acknowledgedAt: '2026-10-12T12:05:00' }]);
    acknowledgeNotification.mockResolvedValue({ ...unreadNotice, acknowledged: true });

    renderPage();
    userEvent.click(await screen.findByText('Got it'));

    await waitFor(() => expect(acknowledgeNotification).toHaveBeenCalledWith(1));
    expect(await screen.findByText('✓ Read')).toBeInTheDocument();
    expect(screen.queryByText('Got it')).not.toBeInTheDocument();
  });

  test('shows the error message when loading fails', async () => {
    getMyNotifications.mockRejectedValue(new Error('Failed to load notices'));

    renderPage();

    expect(await screen.findByText('Failed to load notices')).toBeInTheDocument();
  });
});
