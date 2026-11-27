// ===================== MOCK DATA FOR SPHWMS =====================

export const dashboardProducts = [
  { name: 'Rice (Miniket)', value: 480000 },
  { name: 'Rice (Nazirshail)', value: 340000 },
  { name: 'Potato (Diamond)', value: 260000 },
  { name: 'Lentil (Masur)', value: 230000 },
  { name: 'Mango (Himsagar)', value: 180000 },
  { name: 'Tomato', value: 160000 },
  { name: 'Onion', value: 140000 },
  { name: 'Lentil (Moong)', value: 130000 },
  { name: 'Potato (Cardinal)', value: 110000 },
  { name: 'Wheat', value: 88000 },
  { name: 'Garlic', value: 76000 },
  { name: 'Ginger', value: 65000 },
  { name: 'Chili', value: 55000 },
  { name: 'Mango (Langra)', value: 47000 },
  { name: 'Cauliflower', value: 38000 },
  { name: 'Cabbage', value: 32000 },
  { name: 'Pumpkin', value: 28000 },
  { name: 'Brinjal', value: 22000 },
  { name: 'Peas', value: 18000 },
  { name: 'Carrot', value: 15000 },
];

export const dashboardSummary = {
  totalProducts: 128,
  arrivalsThisMonth: 17,
  stockValue: '24,86,500',
  totalDue: '1,32,000',
};

export const products = [
  { code: 'P-001', name: 'Rice (Miniket)', category: 'Grain', purchase: 52, selling: 60, availableIn: 'A: 900 kg, B: 300 kg', entry: '02 Nov 2026', expiry: '28 Oct 2027', status: 'In Stock' },
  { code: 'P-002', name: 'Tomato', category: 'Vegetable', purchase: 18, selling: 26, availableIn: 'B: 120 kg', entry: '16 Nov 2026', expiry: '26 Nov 2026', status: 'Near Expiry' },
  { code: 'P-003', name: 'Lentil (Masur)', category: 'Pulse', purchase: 95, selling: 110, availableIn: 'A: 40 kg', entry: '08 Oct 2026', expiry: '21 May 2027', status: 'Low Stock' },
  { code: 'P-004', name: 'Mango', category: 'Fruit', purchase: 70, selling: 95, availableIn: 'Not available', entry: '23 Oct 2026', expiry: '19 Nov 2026', status: 'Out of Stock' },
  { code: 'P-005', name: 'Potato', category: 'Vegetable', purchase: 22, selling: 30, availableIn: 'A: 2,000 kg', entry: '10 Nov 2026', expiry: '05 Feb 2027', status: 'In Stock' },
];

export const categories = ['Grain', 'Vegetable', 'Fruit', 'Pulse', 'Others'];

export const arrivals = [
  { batchNo: 'B-2026-117', product: 'Rice (Miniket)', warehouse: 'A', quantity: '500 kg', expiry: '27 Oct 2027', totalCost: 26000, status: 'Pending' },
  { batchNo: 'B-2026-116', product: 'Tomato', warehouse: 'B', quantity: '200 kg', expiry: '27 Nov 2026', totalCost: 3600, status: 'Confirmed' },
  { batchNo: 'B-2026-115', product: 'Lentil (Masur)', warehouse: 'A', quantity: '150 kg', expiry: '20 May 2027', totalCost: 14250, status: 'Rejected' },
];

export const buyers = [
  { name: 'Sumon Traders', phone: '01711-123456', address: 'Kawran Bazar, Dhaka', type: 'Wholesaler', totalPurchase: 59000, totalPaid: 30000, totalDue: 29000, lastPurchase: '18 Nov 2026' },
  { name: 'Rahman Store', phone: '01811-654321', address: 'Mirpur, Dhaka', type: 'Retailer', totalPurchase: 42000, totalPaid: 42000, totalDue: 0, lastPurchase: '15 Nov 2026' },
  { name: 'Green Valley Restaurant', phone: '01911-789012', address: 'Gulshan, Dhaka', type: 'Restaurant', totalPurchase: 31000, totalPaid: 20000, totalDue: 11000, lastPurchase: '12 Nov 2026' },
];

export const purchaseHistory = [
  { saleNo: 'S-0412', date: '18 Nov', product: 'Rice (Miniket)', buyer: 'Sumon Traders', phone: '01711-123456', address: 'Kawran Bazar, Dhaka', qty: '300 kg', total: 18000, status: 'Due' },
  { saleNo: 'S-0411', date: '15 Nov', product: 'Potato', buyer: 'Rahman Store', phone: '01811-654321', address: 'Mirpur, Dhaka', qty: '200 kg', total: 6000, status: 'Paid' },
  { saleNo: 'S-0410', date: '12 Nov', product: 'Tomato', buyer: 'Green Valley Restaurant', phone: '01911-789012', address: 'Gulshan, Dhaka', qty: '150 kg', total: 3900, status: 'Due' },
  { saleNo: 'S-0409', date: '10 Nov', product: 'Lentil (Masur)', buyer: 'Sumon Traders', phone: '01711-123456', address: 'Kawran Bazar, Dhaka', qty: '100 kg', total: 11000, status: 'Paid' },
];

export const donations = [
  { no: 'D-0021', date: '19 Nov', product: 'Potato', qty: '200 kg', receiver: 'Hope Orphanage', phone: '01811-000111' },
  { no: 'D-0020', date: '14 Nov', product: 'Banana', qty: '80 kg', receiver: 'Care NGO', phone: '01911-000222' },
  { no: 'D-0019', date: '08 Nov', product: 'Tomato', qty: '90 kg', receiver: 'Hope Orphanage', phone: '01811-000111' },
  { no: 'D-0018', date: '03 Nov', product: 'Tomato', qty: '60 kg', receiver: 'Care NGO', phone: '01911-000222' },
  { no: 'D-0017', date: '28 Oct', product: 'Rice (Miniket)', qty: '50 kg', receiver: 'Al-Amin Madrasa', phone: '01711-000333' },
  { no: 'D-0016', date: '20 Oct', product: 'Potato', qty: '70 kg', receiver: 'Al-Amin Madrasa', phone: '01711-000333' },
  { no: 'D-0015', date: '12 Oct', product: 'Banana', qty: '40 kg', receiver: 'Hope Orphanage', phone: '01811-000111' },
];

export const eligibleDonations = [
  { product: 'Tomato', batch: 'B-102', warehouse: 'Warehouse B', qty: '120 kg', expiry: '23 Nov', left: '1 day' },
  { product: 'Banana', batch: 'B-109', warehouse: 'Warehouse A', qty: '60 kg', expiry: '24 Nov', left: '2 days' },
];

export const drivers = [
  { name: 'Karim Uddin', license: 'DL-482910', phone: '01711-000001', vehicle: 'Truck DH-11-2201', status: 'Available' },
  { name: 'Jamal Hossain', license: 'DL-482977', phone: '01711-000002', vehicle: 'Pickup DH-14-5532', status: 'Not Available' },
  { name: 'Rafiq Mia', license: 'DL-483015', phone: '01711-000003', vehicle: 'Truck CH-12-7710', status: 'Available' },
  { name: 'Sohel Rana', license: 'DL-483120', phone: '01711-000004', vehicle: 'Van DH-15-4408', status: 'Available' },
  { name: 'Nasir Ahmed', license: 'DL-483244', phone: '01711-000005', vehicle: 'Truck DH-11-9032', status: 'Not Available' },
  { name: 'Habib Khan', license: 'DL-483301', phone: '01711-000006', vehicle: 'Pickup CH-13-1180', status: 'Available' },
];

export const warehouses = [
  { name: 'A', code: 'WH-01', location: 'Gazipur', inCharge: 'Abdul Karim', capacity: 12000, stockAmount: '14,20,000', used: 62, status: 'Active' },
  { name: 'B', code: 'WH-02', location: 'Narayanganj', inCharge: 'Salma Begum', capacity: 8000, stockAmount: '8,90,000', used: 85, status: 'Active' },
  { name: 'C', code: 'WH-03', location: 'Savar', inCharge: 'Mizan Rahman', capacity: 5000, stockAmount: '1,76,500', used: 34, status: 'Active' },
  { name: 'D', code: 'WH-04', location: 'Tongi', inCharge: 'Rafiq Hasan', capacity: 6000, stockAmount: '0', used: 0, status: 'Inactive' },
];

export const reportKPIs = {
  totalRevenue: '6,40,000',
  totalExpense: '4,10,000',
  netProfit: '2,30,000',
  donatedValue: '18,400',
};

export const revenueByCategory = [
  { name: 'Grain', value: 40, amount: 256000 },
  { name: 'Vegetable', value: 30, amount: 192000 },
  { name: 'Pulse', value: 20, amount: 128000 },
  { name: 'Fruit', value: 10, amount: 64000 },
];

export const topProducts = [
  { name: 'Rice', value: 256000 },
  { name: 'Lentil', value: 128000 },
  { name: 'Potato', value: 121600 },
  { name: 'Tomato', value: 70400 },
  { name: 'Mango', value: 64000 },
];

export const alerts = [
  { type: 'Low Stock', item: 'Lentil (Masur)', warehouse: 'Warehouse A', details: '40 kg ≤ reorder level 50 kg', read: false },
  { type: 'Low Stock', item: 'Mango', warehouse: 'Warehouse B', details: '0 kg ≤ reorder level 30 kg', read: false },
  { type: 'Low Stock', item: 'Onion', warehouse: 'Warehouse A', details: '35 kg ≤ reorder level 100 kg', read: false },
  { type: 'Low Stock', item: 'Garlic', warehouse: 'Warehouse B', details: '20 kg ≤ reorder level 40 kg', read: true },
  { type: 'Low Stock', item: 'Wheat', warehouse: 'Warehouse A', details: '80 kg ≤ reorder level 150 kg', read: true },
  { type: 'Near Expiry', item: 'Tomato', warehouse: 'Warehouse B', details: 'Expires 26 Nov 2026', read: false },
  { type: 'Near Expiry', item: 'Mango', warehouse: 'Warehouse B', details: 'Expired 19 Nov 2026', read: true },
  { type: 'Near Expiry', item: 'Banana', warehouse: 'Warehouse A', details: 'Expires 24 Nov 2026', read: false },
  { type: 'Due Payment', item: 'Sumon Traders', warehouse: '-', details: 'Tk 29,000 due', read: false },
  { type: 'Due Payment', item: 'Green Valley Restaurant', warehouse: '-', details: 'Tk 11,000 due', read: false },
  { type: 'Due Payment', item: 'Noor Traders', warehouse: '-', details: 'Tk 8,500 due', read: true },
  { type: 'Due Payment', item: 'Hasan Enterprise', warehouse: '-', details: 'Tk 5,200 due', read: true },
  { type: 'License Expiry', item: 'Nasir Ahmed', warehouse: '-', details: 'License expires 30 Nov 2026', read: false },
];

export const alertSummary = { lowStock: 5, nearExpiry: 3, duePayment: 4, licenseExpiry: 1 };

export const transportCosts = [
  { trip: 'T-031', date: '15 Nov', driver: 'Karim Uddin', route: 'Dhaka – Gazipur', fuel: 3200, driverCost: 1500, other: 300, total: 5000 },
  { trip: 'T-030', date: '11 Nov', driver: 'Rafiq Mia', route: 'Savar – Dhaka', fuel: 2100, driverCost: 1200, other: 0, total: 3300 },
  { trip: 'T-029', date: '08 Nov', driver: 'Sohel Rana', route: 'Dhaka – Narayanganj', fuel: 2600, driverCost: 1300, other: 150, total: 4050 },
  { trip: 'T-028', date: '04 Nov', driver: 'Karim Uddin', route: 'Dhaka – Tongi', fuel: 1800, driverCost: 1000, other: 0, total: 2800 },
  { trip: 'T-027', date: '28 Oct', driver: 'Rafiq Mia', route: 'Dhaka – Savar', fuel: 2300, driverCost: 1200, other: 200, total: 3700 },
];

export const monthlyTransport = [
  { month: 'Jun', cost: 7200 },
  { month: 'Jul', cost: 7900 },
  { month: 'Aug', cost: 8600 },
  { month: 'Sep', cost: 15600 },
  { month: 'Oct', cost: 16400 },
  { month: 'Nov', cost: 15200 },
];

export const userProfile = {
  name: 'Rahim Uddin',
  role: 'Warehouse Manager',
  id: 'EMP-0007',
  phone: '01712-345678',
  email: 'rahim.uddin@example.com',
  address: 'Gazipur, Dhaka',
};
