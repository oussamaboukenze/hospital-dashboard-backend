import type { LucideIcon } from 'lucide-react';
import styles from './MetricCard.module.css';

interface MetricCardProps {
  label: string;
  value: string;
  hint: string;
  tone: 'teal' | 'blue' | 'amber' | 'red';
  icon: LucideIcon;
}

export default function MetricCard({ label, value, hint, tone, icon: Icon }: MetricCardProps) {
  return (
    <article className={`${styles.card} ${styles[tone]}`}>
      <div className={styles.top}>
        <span>{label}</span>
        <Icon size={19} />
      </div>
      <strong>{value}</strong>
      <p>{hint}</p>
    </article>
  );
}
