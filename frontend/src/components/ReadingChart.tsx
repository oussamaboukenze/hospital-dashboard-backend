import { memo } from 'react';
import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { Reading, Refrigerator } from '../types';

interface ReadingChartProps {
  readings: Reading[];
  fridge?: Refrigerator;
}

function ReadingChart({ readings, fridge }: ReadingChartProps) {
  const data = readings.map((reading) => ({
    time: new Date(reading.recordedAt).toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }),
    temperature: Number(reading.temperature.toFixed(2)),
    humidity: Number(reading.humidity.toFixed(2)),
  }));

  return (
    <ResponsiveContainer width="100%" height={310}>
      <LineChart data={data} margin={{ top: 16, right: 22, bottom: 8, left: 0 }}>
        <CartesianGrid stroke="#e6eef2" vertical={false} />
        <XAxis dataKey="time" tick={{ fontSize: 12, fill: '#607d8b' }} minTickGap={28} />
        <YAxis yAxisId="temp" tick={{ fontSize: 12, fill: '#607d8b' }} domain={[0, 14]} unit="°C" />
        <YAxis yAxisId="hum" orientation="right" tick={{ fontSize: 12, fill: '#607d8b' }} unit="%" />
        <Tooltip
          contentStyle={{
            border: '1px solid #d9e4ea',
            borderRadius: 8,
            boxShadow: '0 10px 25px rgba(16, 32, 39, 0.12)',
          }}
        />
        {fridge && (
          <>
            <ReferenceLine yAxisId="temp" y={fridge.minTemperature} stroke="#1976d2" strokeDasharray="4 4" />
            <ReferenceLine yAxisId="temp" y={fridge.maxTemperature} stroke="#f57f17" strokeDasharray="4 4" />
          </>
        )}
        <Line yAxisId="temp" type="monotone" dataKey="temperature" stroke="#00897b" strokeWidth={3} dot={false} />
        <Line yAxisId="hum" type="monotone" dataKey="humidity" stroke="#1976d2" strokeWidth={2} dot={false} />
      </LineChart>
    </ResponsiveContainer>
  );
}

export default memo(ReadingChart);
