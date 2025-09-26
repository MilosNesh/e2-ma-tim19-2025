import express from 'express';
import admin from 'firebase-admin';
import { readFileSync } from 'fs';

const serviceAccount = JSON.parse(readFileSync('./service-account.json', 'utf8'));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const app = express();
app.use(express.json());

app.post('/send', async (req, res) => {
  const { token, title, body } = req.body;

  try {
    const message = {
      token: token,
      data: {
        action: 'none',
        title: title,
        body: body,
      },
    };

    const response = await admin.messaging().send(message);
    res.json({ success: true, response });
  } catch (error) {
    console.error('Greška pri slanju:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

app.post('/send-invite', async (req, res) => {
  const { token, title, body, allianceId, inviteId, senderEmail } = req.body;

  try {
    const message = {
      token: token,
      data: {
        action: 'invite', 
        allianceId: allianceId,
        inviteId: inviteId,
        title: title, 
        body: body,
        senderEmail: senderEmail,
      },
    };

    const response = await admin.messaging().send(message);
    res.json({ success: true, response });
  } catch (error) {
    console.error('Greška pri slanju data notifikacije:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server sluša na portu ${PORT}`);
});
