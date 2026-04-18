"use client";

import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

export function SalesTrendChart({ data }: { data: { date: string; totalAmount: string; itemCount: number }[] }) {
  const chartData = data.map((point) => ({
    date: point.date,
    totalAmount: Number(point.totalAmount || 0),
    itemCount: point.itemCount,
  }));

  return (
    <div className="h-72 w-full">
      <ResponsiveContainer>
        <AreaChart data={chartData} margin={{ left: 0, right: 16, top: 12, bottom: 0 }}>
          <defs>
            <linearGradient id="sales" x1="0" x2="0" y1="0" y2="1">
              <stop offset="5%" stopColor="#d97706" stopOpacity={0.32} />
              <stop offset="95%" stopColor="#d97706" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
          <XAxis dataKey="date" tick={{ fontSize: 12 }} />
          <YAxis tick={{ fontSize: 12 }} />
          <Tooltip />
          <Area type="monotone" dataKey="totalAmount" stroke="#b45309" fill="url(#sales)" strokeWidth={2} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
