import { z } from "zod";

export const decimalString = (maxScale: number, label: string) =>
  z
    .string()
    .trim()
    .min(1, `${label} is required`)
    .regex(new RegExp(`^\\d+(\\.\\d{1,${maxScale}})?$`), `${label} supports up to ${maxScale} decimal places`);

export const loginSchema = z.object({
  email: z.string().trim().email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});

export const registerSchema = z.object({
  shopName: z.string().trim().min(1, "Shop name is required").max(160),
  ownerName: z.string().trim().min(1, "Owner name is required").max(140),
  email: z.string().trim().email("Enter a valid email").max(180),
  password: z.string().min(8, "Password must be at least 8 characters").max(100),
  billPrefix: z.string().trim().min(1, "Bill prefix is required").max(20),
  defaultCurrencyCode: z.string().trim().length(3, "Use a 3-letter ISO currency code").toUpperCase(),
});

export const jewellerySchema = z.object({
  typeId: z.string().min(1, "Select a jewellery type"),
  karat: z.string().trim().regex(/^[0-9]{1,2}K$/i, "Use values like 24K, 22K, or 18K"),
  designName: z.string().trim().max(160, "Design name must be 160 characters or less").optional(),
  weight: decimalString(3, "Weight"),
  notes: z.string().trim().max(1000, "Notes must be 1000 characters or less").optional(),
});

export const staffSchema = z.object({
  name: z.string().trim().min(1, "Name is required").max(140),
  email: z.string().trim().email("Enter a valid email").max(180),
  password: z.string().min(8, "Password must be at least 8 characters").max(100),
});

export const moneySchema = decimalString(2, "Amount");
