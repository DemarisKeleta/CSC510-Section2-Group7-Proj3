import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import AllergyIncidents from './AllergyIncidents';
import {
  getCurrentUser,
  getAllFoods,
  getIncidents,
  reportRestaurantIncident,
  acknowledgeIncident,
} from '../services/api';

jest.mock('../services/api', () => ({
  getCurrentUser: jest.fn(),
  getAllFoods: jest.fn(),
  getIncidents: jest.fn(),
  reportRestaurantIncident: jest.fn(),
  acknowledgeIncident: jest.fn(),
}));

const openIncident = {
  id: 4,
  source: 'CUSTOMER',
  allergen: 'PEANUT',
  sourceOrderId: 8,
  suspectFoodIds: [30],
  suspectFoodNames: ['COOKIES'],
  windowStart: '2026-10-12T10:00:00',
  windowEnd: '2026-10-12T14:00:00',
  reportedAt: '2026-10-12T12:30:00',
  exposedOrderCount: 5,
  notifiedCount: 2,
  restaurantAcknowledgedAt: null,
  acknowledgedBy: null,
  remedy: null,
};

const renderPage = () => render(
  <MemoryRouter>
    <AllergyIncidents />
  </MemoryRouter>
);

describe('AllergyIncidents', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    getCurrentUser.mockResolvedValue({ username: 'staff', role: 'ROLE_STAFF' });
    getAllFoods.mockResolvedValue([
      { id: 30, foodName: 'COOKIES' },
      { id: 31, foodName: 'FRUIT SALAD' },
    ]);
    getIncidents.mockResolvedValue([openIncident]);
  });

  test('lists incidents with counts and no customer names', async () => {
    renderPage();

    expect(await screen.findByText('Incident #4: PEANUT')).toBeInTheDocument();
    expect(screen.getByText(/a customer, after order #8/)).toBeInTheDocument();
    expect(screen.getByText('⏳ Needs a response')).toBeInTheDocument();
  });

  test('requires an allergen before reporting', async () => {
    renderPage();

    userEvent.click(await screen.findByText('Trace and warn customers'));

    expect(await screen.findByText('Please enter the allergen.')).toBeInTheDocument();
    expect(reportRestaurantIncident).not.toHaveBeenCalled();
  });

  test('requires at least one dish before reporting', async () => {
    renderPage();

    userEvent.type(await screen.findByPlaceholderText('For example: Peanuts'), 'Peanuts');
    userEvent.click(screen.getByText('Trace and warn customers'));

    expect(await screen.findByText('Please select at least one dish.')).toBeInTheDocument();
    expect(reportRestaurantIncident).not.toHaveBeenCalled();
  });

  test('reports an incident and shows the counts', async () => {
    reportRestaurantIncident.mockResolvedValue({ incidentId: 9, exposedOrderCount: 3, notifiedCount: 1 });

    renderPage();
    userEvent.type(await screen.findByPlaceholderText('For example: Peanuts'), 'Peanuts');
    userEvent.click(screen.getByLabelText('COOKIES'));
    userEvent.click(screen.getByText('Trace and warn customers'));

    expect(await screen.findByText(/Incident #9 saved\. 3 orders fell in the window, and 1 customer/))
      .toBeInTheDocument();
    expect(reportRestaurantIncident).toHaveBeenCalledWith('Peanuts', [30], expect.any(String), expect.any(String));
  });

  test('shows the backend message when reporting fails', async () => {
    reportRestaurantIncident.mockRejectedValue(new Error('The window start must not be after the window end.'));

    renderPage();
    userEvent.type(await screen.findByPlaceholderText('For example: Peanuts'), 'Peanuts');
    userEvent.click(screen.getByLabelText('COOKIES'));
    userEvent.click(screen.getByText('Trace and warn customers'));

    expect(await screen.findByText('The window start must not be after the window end.')).toBeInTheDocument();
  });

  test('acknowledging needs an offer, then sends it', async () => {
    acknowledgeIncident.mockResolvedValue({ ...openIncident, remedy: 'Full refund' });

    renderPage();
    userEvent.click(await screen.findByText('Acknowledge'));
    expect(await screen.findByText(/Please describe what the restaurant offers/)).toBeInTheDocument();
    expect(acknowledgeIncident).not.toHaveBeenCalled();

    userEvent.type(screen.getByPlaceholderText(/What do you offer/), 'Full refund');
    userEvent.click(screen.getByText('Acknowledge'));

    await waitFor(() => expect(acknowledgeIncident).toHaveBeenCalledWith(4, 'Full refund'));
  });
});
